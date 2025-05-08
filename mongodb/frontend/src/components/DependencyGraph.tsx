import React, { useCallback, useMemo, useState } from 'react';
import ReactFlow, { 
  Node, 
  Edge,
  Background,
  Controls,
  MiniMap,
  Position,
  useNodesState,
  useEdgesState,
  MarkerType,
  NodeMouseHandler
} from 'reactflow';
import 'reactflow/dist/style.css';
import dagre from 'dagre';
import { Method, Class } from '../types';
import Badge from './Badge';

interface DependencyGraphProps {
  callGraph: {
    nodes: Array<{
      id: string;
      type: string;
    }>;
    edges: Array<{
      from: string;
      to: string;
    }>;
  };
  methods?: Method[];
  classes?: Class[];
  nodeType?: 'method' | 'class'; // 添加节点类型属性，用于区分方法图和类图
}

// Node distance for layout
const NODE_WIDTH = 250;
const NODE_HEIGHT = 50;

// Utility function to get a short readable name for display
const getDisplayName = (fullName: string) => {
  const parts = fullName.split('.');
  // If it's a method, show className.methodName
  if (parts.length > 2) {
    const methodName = parts[parts.length - 1];
    const className = parts[parts.length - 2];
    return `${className}.${methodName}`;
  }
  return parts[parts.length - 1];
};

// Get node color based on type and properties
const getNodeColor = (nodeType: string, called: boolean, unused: boolean, framework: boolean) => {
  if (nodeType !== 'method') return '#f8fafc'; // Default for non-methods
  if (unused) return '#fee2e2'; // Light red for unused methods
  if (!called) return '#fef3c7'; // Light yellow for not called methods
  if (framework) return '#e0f2fe'; // Light blue for framework methods
  return '#dcfce7'; // Light green for regular used methods
};

// Gets the color for an edge
const getEdgeColor = (source: string, target: string, highlighted: boolean = false) => {
  if (highlighted) return '#f43f5e'; // Highlight color for connections
  
  // Providing different colors for better visual distinction between edge types
  // This uses a hash-like approach to get consistent colors
  const hash = (source.length + target.length) % 5;
  const colors = ['#60a5fa', '#a78bfa', '#f87171', '#34d399', '#fbbf24'];
  return colors[hash];
};

// Dagre layout algorithm
const getLayoutedElements = (nodes: Node[], edges: Edge[], direction = 'LR') => {
  const dagreGraph = new dagre.graphlib.Graph();
  dagreGraph.setDefaultEdgeLabel(() => ({}));
  dagreGraph.setGraph({ rankdir: direction });

  // Add nodes to dagre
  nodes.forEach((node) => {
    dagreGraph.setNode(node.id, { width: NODE_WIDTH, height: NODE_HEIGHT });
  });

  // Add edges to dagre
  edges.forEach((edge) => {
    dagreGraph.setEdge(edge.source, edge.target);
  });

  // Calculate layout
  dagre.layout(dagreGraph);

  // Apply layout to nodes
  return nodes.map((node) => {
    const nodeWithPosition = dagreGraph.node(node.id);
    return {
      ...node,
      position: {
        x: nodeWithPosition.x - NODE_WIDTH / 2,
        y: nodeWithPosition.y - NODE_HEIGHT / 2,
      },
    };
  });
};

const DependencyGraph: React.FC<DependencyGraphProps> = ({ 
  callGraph, 
  methods = [],
  classes = [],
  nodeType = 'method' 
}) => {
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedNode, setSelectedNode] = useState<any | null>(null);
  const [highlightedNodeId, setHighlightedNodeId] = useState<string | null>(null);
  
  // Create initial elements
  const initialElements = useMemo(() => {
    // Filter nodes, keep reasonable number
    const topNodes = callGraph.nodes
      .filter(node => node.type === nodeType)
      .slice(0, 100); 
    
    const nodeIds = new Set(topNodes.map(node => node.id));
    
    // just keep edges connected to the nodes we show
    const relevantEdges = callGraph.edges.filter(
      edge => nodeIds.has(edge.from) && nodeIds.has(edge.to)
    ).slice(0, 200); 
    
    // Create nodes for react-flow
    const graphNodes: Node[] = topNodes.map((node) => {
      if (nodeType === 'class') {
        // Process class nodes
        const classData = classes.find(cls => cls.id === node.id);
        
        // Confirm class node properties
        const unused = classData?.unused ?? false;
        const framework = classData?.framework ?? false;
        const test = classData?.test ?? false;
        
        return {
          id: node.id,
          data: { 
            label: getDisplayName(node.id),
            fullName: node.id,
            nodeType: 'class'
          },
          position: { x: 0, y: 0 },
          sourcePosition: Position.Right,
          targetPosition: Position.Left,
          style: {
            background: getNodeColor(node.type, true, unused, framework),
            padding: 10,
            borderRadius: 8,
            border: '1px solid #cbd5e1',
            width: NODE_WIDTH,
            boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
            fontSize: '12px',
            fontWeight: 500
          },
        };
      } else {
        // Deal with method nodes
        const methodData = methods.find(method => method.fullName === node.id);
        
        // Use actual properties
        const called = methodData?.called ?? false;
        const unused = methodData?.unused ?? false;
        const framework = methodData?.framework ?? false;
        
        return {
          id: node.id,
          data: { 
            label: getDisplayName(node.id),
            fullName: node.id,
            nodeType: 'method'
          },
          position: { x: 0, y: 0 },
          sourcePosition: Position.Right,
          targetPosition: Position.Left,
          style: {
            background: getNodeColor(node.type, called, unused, framework),
            padding: 10,
            borderRadius: 8,
            border: '1px solid #cbd5e1',
            width: NODE_WIDTH,
            boxShadow: '0 2px 4px rgba(0,0,0,0.1)',
            fontSize: '12px',
            fontWeight: 500
          },
        };
      }
    });

    // Create edges for react-flow
    const graphEdges: Edge[] = relevantEdges.map((edge, index) => ({
      id: `edge-${index}`,
      source: edge.from,
      target: edge.to,
      type: 'smoothstep',
      animated: false,
      style: { stroke: getEdgeColor(edge.from, edge.to), strokeWidth: 2 },
      markerEnd: {
        type: MarkerType.ArrowClosed,
        color: getEdgeColor(edge.from, edge.to),
        width: 15,
        height: 15,
      },
    }));

    // Layout nodes
    const nodesWithLayout = getLayoutedElements(graphNodes, graphEdges);
    
    return { nodes: nodesWithLayout, edges: graphEdges };
  }, [callGraph, methods, classes, nodeType]);

  // Use react-flow hooks to manage state
  const [nodes, setNodes, onNodesChange] = useNodesState(initialElements.nodes);
  const [edges, setEdges, onEdgesChange] = useEdgesState(initialElements.edges);
  
  // Function to get connected node IDs (inbound and outbound)
  const getConnectedNodeIds = useCallback((nodeId: string) => {
    const connectedIds = new Set<string>();
    
    // Add source node itself
    connectedIds.add(nodeId);
    
    // Check edges for connected nodes
    initialElements.edges.forEach(edge => {
      if (edge.source === nodeId) {
        connectedIds.add(edge.target);
      }
      // For class graph, also show inbound connections
      if (nodeType === 'class' && edge.target === nodeId) {
        connectedIds.add(edge.source);
      }
    });
    
    return connectedIds;
  }, [initialElements.edges, nodeType]);
  
  // Update highlighted elements
  const updateHighlightedElements = useCallback((nodeId: string | null) => {
    if (!nodeId) {
      // If there's no highlighted node, reset to original state
      setNodes(nodes =>
        nodes.map(node => ({
          ...node,
          style: {
            ...node.style,
            opacity: 1,
            borderWidth: '1px',
            borderColor: '#cbd5e1',
          },
        }))
      );
      
      setEdges(edges =>
        edges.map(edge => ({
          ...edge,
          style: {
            ...edge.style,
            stroke: getEdgeColor(edge.source, edge.target),
            strokeWidth: 2,
            opacity: 1,
          },
          markerEnd: {
            ...edge.markerEnd,
            color: getEdgeColor(edge.source, edge.target),
          },
        }))
      );
      
      return;
    }
    
    // Get connected node IDs
    const connectedIds = getConnectedNodeIds(nodeId);
    
    // Update node styles based on connection status
    setNodes(nodes =>
      nodes.map(node => {
        const isConnected = connectedIds.has(node.id);
        return {
          ...node,
          style: {
            ...node.style,
            opacity: isConnected ? 1 : 0.25,
            borderWidth: node.id === nodeId ? '2px' : '1px',
            borderColor: node.id === nodeId ? '#f43f5e' : '#cbd5e1',
            boxShadow: node.id === nodeId ? '0 0 8px #f43f5e' : node.style?.boxShadow,
          },
        };
      })
    );
    
    // Update edge styles based on connection status
    setEdges(edges =>
      edges.map(edge => {
        const isConnected = edge.source === nodeId || edge.target === nodeId;
        const highlightColor = isConnected ? '#f43f5e' : getEdgeColor(edge.source, edge.target);
        
        return {
          ...edge,
          style: {
            ...edge.style,
            stroke: highlightColor,
            strokeWidth: isConnected ? 3 : 2,
            opacity: isConnected || (connectedIds.has(edge.source) && connectedIds.has(edge.target)) ? 1 : 0.25,
          },
          markerEnd: {
            ...edge.markerEnd,
            color: highlightColor,
          },
        };
      })
    );
  }, [getConnectedNodeIds]);
  
  // Handle search filtering
  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearchTerm(e.target.value);
    
    if (!e.target.value) {
      // Initial state
      setNodes(initialElements.nodes);
      setEdges(initialElements.edges);
      return;
    }
    
    // Filter nodes based on search term
    const term = e.target.value.toLowerCase();
    const filteredNodes = initialElements.nodes.filter(
      node => node.data.fullName.toLowerCase().includes(term)
    );
    
    // Get node IDs
    const nodeIds = new Set(filteredNodes.map(node => node.id));
    
    // Filter edges connected to the nodes we show
    const filteredEdges = initialElements.edges.filter(
      edge => nodeIds.has(edge.source) && nodeIds.has(edge.target)
    );
    
    setNodes(filteredNodes);
    setEdges(filteredEdges);
    
    // Eliminate highlight
    setHighlightedNodeId(null);
  };

  // Handle node click to show details and highlight connections
  const onNodeClick: NodeMouseHandler = useCallback((event, node) => {
    if (nodeType === 'class') {
      const classData = classes.find(cls => cls.id === node.id);
      if (classData) {
        setSelectedNode(classData);
        
        if (highlightedNodeId === node.id) {
          setHighlightedNodeId(null);
          updateHighlightedElements(null);
        } else {
          setHighlightedNodeId(node.id);
          updateHighlightedElements(node.id);
        }
      }
    } else {
      const methodData = methods.find(method => method.fullName === node.id);
      if (methodData) {
        setSelectedNode(methodData);
        
        // The same highlight logic
        if (highlightedNodeId === node.id) {
          setHighlightedNodeId(null);
          updateHighlightedElements(null);
        } else {
          setHighlightedNodeId(node.id);
          updateHighlightedElements(node.id);
        }
      }
    }
  }, [nodeType, methods, classes, highlightedNodeId, updateHighlightedElements]);

  // Close details panel
  const closeDetails = () => {
    setSelectedNode(null);
  };
  
  // Eliminate highlight
  const clearHighlighting = () => {
    setHighlightedNodeId(null);
    updateHighlightedElements(null);
  };

  return (
    <div className="flex flex-col h-[700px] bg-white rounded-lg shadow-lg relative">
      <div className="p-4 border-b flex items-center justify-between">
        <input
          type="text"
          value={searchTerm}
          onChange={handleSearch}
          placeholder={`Search ${nodeType}s...`}
          className="w-full p-2 border rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        
        {highlightedNodeId && (
          <button
            onClick={clearHighlighting}
            className="ml-2 px-3 py-2 bg-gray-100 hover:bg-gray-200 rounded-md text-sm text-gray-700 flex items-center"
          >
            Clear Highlighting
          </button>
        )}
      </div>
      <div className="flex-1">
        <ReactFlow
          nodes={nodes}
          edges={edges}
          onNodesChange={onNodesChange}
          onEdgesChange={onEdgesChange}
          onNodeClick={onNodeClick}
          fitView
          fitViewOptions={{ padding: 0.2 }}
          attributionPosition="bottom-right"
          minZoom={0.2}
          maxZoom={1.5}
          defaultZoom={0.8}
        >
          <Background color="#f1f5f9" gap={16} size={1} />
          <Controls />
          <MiniMap 
            nodeColor={(node) => {
              return node.style?.background as string || '#e2e8f0';
            }}
            maskColor="rgba(0, 0, 0, 0.1)"
          />
        </ReactFlow>
      </div>

      {/* Node details popup */}
      {selectedNode && (
        <div className="absolute top-24 right-8 w-80 bg-white rounded-lg shadow-xl border border-gray-200 z-10">
          <div className={`border-b px-4 py-3 rounded-t-lg flex justify-between items-center ${
            nodeType === 'class' 
              ? 'bg-gradient-to-r from-blue-500 to-blue-600' 
              : 'bg-gradient-to-r from-indigo-500 to-indigo-600'
          }`}>
            <h3 className="font-bold text-white">
              {nodeType === 'class' 
                ? selectedNode.simpleName 
                : selectedNode.name}
            </h3>
            <button 
              onClick={closeDetails}
              className="text-white hover:text-blue-100"
            >
              &times;
            </button>
          </div>
          <div className="p-4">
            <p className="text-xs text-gray-500 mb-2 font-mono truncate" 
               title={nodeType === 'class' ? selectedNode.fullName : selectedNode.fullName}>
              {nodeType === 'class' ? selectedNode.fullName : selectedNode.fullName}
            </p>
            <div className="flex flex-wrap gap-2 mb-4">
              {selectedNode.framework && <Badge text="Framework" type="framework" />}
              {selectedNode.test && <Badge text="Test" type="test" />}
              <Badge 
                text={selectedNode.unused ? "Unused" : "Used"} 
                type={selectedNode.unused ? "unused" : "used"} 
              />
              {nodeType === 'method' && (
                <Badge 
                  text={selectedNode.called ? "Called" : "Not Called"} 
                  type={selectedNode.called ? "used" : "unused"} 
                />
              )}
            </div>
            
            {nodeType === 'method' && selectedNode.calls && selectedNode.calls.length > 0 && (
              <div className="mt-2">
                <h4 className="font-medium text-gray-700 text-sm">Method Calls ({selectedNode.calls.length})</h4>
                <div className="mt-1 p-2 bg-gray-50 rounded-md max-h-40 overflow-y-auto overflow-x-auto">
                  <ul className="space-y-1">
                    {selectedNode.calls.slice(0, 100).map((call: string, index: number) => (
                      <li key={index} className="text-xs font-mono text-gray-700 whitespace-nowrap" title={call}>
                        {call}
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            )}
            
            {nodeType === 'class' && selectedNode.dependsOn && selectedNode.dependsOn.length > 0 && (
              <div className="mt-2">
                <h4 className="font-medium text-gray-700 text-sm">Dependencies ({selectedNode.dependsOn.length})</h4>
                <div className="mt-1 p-2 bg-gray-50 rounded-md max-h-40 overflow-y-auto overflow-x-auto">
                  <ul className="space-y-1">
                    {selectedNode.dependsOn.slice(0, 100).map((dep: any, index: number) => (
                      <li key={index} className="text-xs font-mono text-gray-700 whitespace-nowrap" title={dep.target}>
                        {dep.target} <span className="text-gray-500">({dep.type})</span>
                      </li>
                    ))}
                  </ul>
                </div>
              </div>
            )}
          </div>
        </div>
      )}
      
      <div className="p-3 bg-gray-50 border-t text-xs text-gray-500">
        <div className="flex items-center justify-center space-x-4">
          {nodeType === 'method' ? (
            <>
              <span className="flex items-center">
                <span className="inline-block w-3 h-3 mr-1 bg-[#dcfce7] rounded-full"></span>
                Used Methods
              </span>
              <span className="flex items-center">
                <span className="inline-block w-3 h-3 mr-1 bg-[#e0f2fe] rounded-full"></span>
                Framework Methods
              </span>
              <span className="flex items-center">
                <span className="inline-block w-3 h-3 mr-1 bg-[#fef3c7] rounded-full"></span>
                Not Called
              </span>
            </>
          ) : (
            <>
              <span className="flex items-center">
                <span className="inline-block w-3 h-3 mr-1 bg-[#dcfce7] rounded-full"></span>
                Used Classes
              </span>
              <span className="flex items-center">
                <span className="inline-block w-3 h-3 mr-1 bg-[#e0f2fe] rounded-full"></span>
                Framework Classes
              </span>
            </>
          )}
          <span className="flex items-center">
            <span className="inline-block w-3 h-3 mr-1 bg-[#fee2e2] rounded-full"></span>
            Unused
          </span>
          {highlightedNodeId && (
            <span className="flex items-center">
              <span className="inline-block w-3 h-3 mr-1 bg-[#f43f5e] rounded-full"></span>
              Highlighted Connections
            </span>
          )}
        </div>
      </div>
    </div>
  );
};

export default DependencyGraph;