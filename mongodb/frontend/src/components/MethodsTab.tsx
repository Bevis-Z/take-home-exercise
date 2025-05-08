import React, { useState, useMemo, useEffect } from 'react';
import { Filter, Info, ArrowDown, ArrowUp, AlertTriangle } from 'lucide-react';
import { Class, Method, CallGraph, ImpactAnalysis } from '../types';
import Badge from './Badge';
import SearchBar from './SearchBar';
import DependencyGraph from './DependencyGraph';

interface MethodsTabProps {
  classes: Class[];
  methods: Method[];
  callGraph: CallGraph;
  impactAnalysis: ImpactAnalysis[];
}

const MethodsTab: React.FC<MethodsTabProps> = ({ 
  classes = [], 
  methods = [], 
  callGraph = { nodes: [], edges: [] },
  impactAnalysis = [] 
}) => {
  const [showUnusedOnly, setShowUnusedOnly] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedMethod, setSelectedMethod] = useState<string | null>(null);
  const [sortField, setSortField] = useState<'name' | 'class' | 'calls' | 'impact'>('calls');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');
  const [view, setView] = useState<'list' | 'graph'>('list');

  const allMethods = useMemo(() => {
    if (!Array.isArray(methods)) return [];
    
    return methods.map(method => {
      const classObj = classes.find(cls => cls.id === method.declaringClass);
      const impact = impactAnalysis.find(
        impact => impact.type === 'method' && impact.method === method.fullName
      );
      
      return {
        method,
        className: classObj?.simpleName || method.declaringClass.split('.').pop() || '',
        classId: method.declaringClass,
        impactTotal: impact?.impactRadius.totalImpact || 0
      };
    });
  }, [classes, methods, impactAnalysis]);

  const handleSort = (field: 'name' | 'class' | 'calls' | 'impact') => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection(field === 'calls' || field === 'impact' ? 'desc' : 'asc');
    }
  };

  const filteredMethods = useMemo(() => {
    return allMethods
      .filter(item => !showUnusedOnly || item.method.unused)
      .filter(item => {
        if (!searchTerm) return true;
        
        const lowercaseSearchTerm = searchTerm.toLowerCase();
        
        // Search by method name
        if (item.method.name.toLowerCase().includes(lowercaseSearchTerm)) {
          return true;
        }
        
        // Search by fully qualified method name (className.methodName)
        if (item.method.fullName.toLowerCase().includes(lowercaseSearchTerm)) {
          return true;
        }
        
        // Search by class name
        if (item.className.toLowerCase().includes(lowercaseSearchTerm)) {
          return true;
        }
        
        // Search by declaring class (fully qualified class name)
        if (item.method.declaringClass.toLowerCase().includes(lowercaseSearchTerm)) {
          return true;
        }
        
        return false;
      })
      .sort((a, b) => {
        if (sortField === 'calls') {
          const callsA = a.method.calls?.length || 0;
          const callsB = b.method.calls?.length || 0;
          return sortDirection === 'asc' ? callsA - callsB : callsB - callsA;
        }
        
        if (sortField === 'impact') {
          return sortDirection === 'asc' ? a.impactTotal - b.impactTotal : b.impactTotal - a.impactTotal;
        }
        
        const fieldA = sortField === 'name' ? a.method.name : a.className;
        const fieldB = sortField === 'name' ? b.method.name : b.className;
        
        if (sortDirection === 'asc') {
          return fieldA.localeCompare(fieldB);
        } else {
          return fieldB.localeCompare(fieldA);
        }
      });
  }, [allMethods, showUnusedOnly, searchTerm, sortField, sortDirection]);

  const selectedMethodData = useMemo(() => {
    return allMethods.find(item => item.method.fullName === selectedMethod);
  }, [allMethods, selectedMethod]);

  const relatedClass = useMemo(() => {
    if (!selectedMethodData) return null;
    return classes.find(cls => cls.id === selectedMethodData.classId);
  }, [classes, selectedMethodData]);

  const renderSortIcon = (field: 'name' | 'class' | 'calls' | 'impact') => {
    if (sortField !== field) return null;
    
    return sortDirection === 'asc' ? (
      <ArrowUp className="h-4 w-4 text-gray-500" />
    ) : (
      <ArrowDown className="h-4 w-4 text-gray-500" />
    );
  };

  // Find impact analysis for selected method
  const selectedMethodImpact = useMemo(() => {
    if (!selectedMethod) return null;
    return impactAnalysis.find(
      impact => impact.type === 'method' && impact.method === selectedMethod
    );
  }, [selectedMethod, impactAnalysis]);

  return (
    <div className="container mx-auto px-4 py-6">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between mb-6 space-y-4 md:space-y-0">
        <h2 className="text-2xl font-bold text-gray-800">Method Analysis</h2>
        
        <div className="flex flex-col sm:flex-row space-y-2 sm:space-y-0 sm:space-x-4">
          <SearchBar 
            searchTerm={searchTerm} 
            setSearchTerm={setSearchTerm} 
            placeholder="Search by method or class name..." 
          />
          
          <button
            onClick={() => setShowUnusedOnly(!showUnusedOnly)}
            className={`flex items-center px-4 py-2 rounded-md border ${
              showUnusedOnly 
                ? 'bg-blue-600 text-white border-blue-600' 
                : 'bg-white text-gray-700 border-gray-300 hover:bg-gray-50'
            }`}
          >
            <Filter className="h-4 w-4 mr-2" />
            {showUnusedOnly ? 'Show All' : 'Show Unused Only'}
          </button>

          <button
            onClick={() => setView(view === 'list' ? 'graph' : 'list')}
            className="flex items-center px-4 py-2 rounded-md border bg-white text-gray-700 border-gray-300 hover:bg-gray-50"
          >
            {view === 'list' ? 'Show Graph' : 'Show List'}
          </button>
        </div>
      </div>

      {view === 'graph' ? (
        <DependencyGraph callGraph={callGraph} methods={methods} />
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-1 bg-white rounded-lg shadow-md overflow-hidden">
            <div className="border-b px-4 py-3 bg-gray-50">
              <h3 className="font-semibold text-gray-700">Method List</h3>
              <p className="text-sm text-gray-500">
                {filteredMethods.length} of {allMethods.length} methods
              </p>
            </div>
            
            <div className="bg-white">
              <div className="grid grid-cols-12 px-4 py-2 border-b text-sm font-medium text-gray-500">
                <div className="col-span-4 flex items-center cursor-pointer" onClick={() => handleSort('name')}>
                  <span>Method Name</span>
                  {renderSortIcon('name')}
                </div>
                <div className="col-span-3 flex items-center cursor-pointer" onClick={() => handleSort('class')}>
                  <span>Class</span>
                  {renderSortIcon('class')}
                </div>
                <div className="col-span-2 flex items-center cursor-pointer" onClick={() => handleSort('calls')}>
                  <span>Calls</span>
                  {renderSortIcon('calls')}
                </div>
                <div className="col-span-3 flex items-center cursor-pointer" onClick={() => handleSort('impact')}>
                  <span>Impact</span>
                  {renderSortIcon('impact')}
                </div>
              </div>
              
              <div className="divide-y divide-gray-100 max-h-[60vh] overflow-y-auto">
                {filteredMethods.length > 0 ? (
                  filteredMethods.map((item) => (
                    <div
                      key={item.method.fullName}
                      className={`grid grid-cols-12 px-4 py-3 text-sm cursor-pointer hover:bg-gray-50 transition-colors ${
                        selectedMethod === item.method.fullName ? 'bg-blue-50' : ''
                      }`}
                      onClick={() => setSelectedMethod(item.method.fullName)}
                    >
                      <div className="col-span-4 flex flex-col">
                        <div className="font-medium text-gray-800 truncate font-mono" title={item.method.name}>
                          {item.method.name}
                        </div>
                        <div className="flex mt-1 space-x-1">
                          {item.method.framework && <Badge text="Inject&Rest" type="framework" />}
                          {item.method.test && <Badge text="TestAnnotation" type="test" />}
                          <Badge text={item.method.unused ? "Unused" : "Used"} type={item.method.unused ? "unused" : "used"} />
                        </div>
                      </div>
                      <div className="col-span-3 text-gray-600 truncate" title={item.className}>
                        {item.className}
                      </div>
                      <div className="col-span-2 flex items-center justify-center">
                        <div className="w-20 h-10 flex items-center justify-center bg-gray-100 rounded-full px-2">
                          <span className="text-md font-mono">{item.method.calls?.length || 0}</span>
                        </div>
                      </div>
                      <div className="col-span-3 flex items-center justify-center">
                        <div className={`w-10 h-10 flex items-center justify-center rounded-full 
                          ${item.impactTotal > 20 ? 'bg-red-100 text-red-800' :
                            item.impactTotal > 10 ? 'bg-amber-100 text-amber-800' :
                            item.impactTotal > 5 ? 'bg-yellow-100 text-yellow-800' :
                            'bg-green-100 text-green-800'}`}>
                          <span className="text-md font-mono">{item.impactTotal}</span>
                        </div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="px-4 py-8 text-center text-gray-500">
                    No methods found matching your criteria.
                  </div>
                )}
              </div>
            </div>
          </div>
          
          <div className="lg:col-span-2">
            {selectedMethodData ? (
              <div className="space-y-6">
                <div className="bg-white rounded-lg shadow-md overflow-hidden">
                  <div className="border-b px-6 py-4 bg-gradient-to-r from-indigo-500 to-indigo-600">
                    <h3 className="font-bold text-xl text-white font-mono">{selectedMethodData.method.name}()</h3>
                    <p className="text-indigo-100 mt-1">{selectedMethodData.method.fullName}</p>
                  </div>
                  
                  <div className="p-6">
                    <div className="flex flex-wrap gap-2 mb-4">
                      {selectedMethodData.method.framework && <Badge text="Inject&Rest" type="framework" />}
                      {selectedMethodData.method.test && <Badge text="TestAnnotation" type="test" />}
                      <Badge 
                        text={selectedMethodData.method.unused ? "Unused" : "Used"} 
                        type={selectedMethodData.method.unused ? "unused" : "used"} 
                      />
                      <Badge 
                        text={selectedMethodData.method.called ? "Called" : "Not Called"} 
                        type={selectedMethodData.method.called ? "used" : "unused"} 
                      />
                      
                      {relatedClass && (
                        <div className="ml-2 text-sm text-gray-600">
                          <span className="font-medium">Class:</span> {relatedClass.simpleName}
                        </div>
                      )}
                      
                      <div className="text-sm text-gray-600">
                        <span className="font-medium">Outgoing Calls:</span> {selectedMethodData.method.calls?.length || 0}
                      </div>
                      
                      {/* Add impact info */}
                      {selectedMethodImpact && (
                        <div className="ml-2 text-sm text-gray-600">
                          <span className="font-medium">Impact:</span> {selectedMethodImpact.impactRadius.totalImpact} methods
                        </div>
                      )}
                    </div>
                    
                    {/* Add Impact Analysis Section */}
                    {selectedMethodImpact && (
                      <div className="mt-4 p-4 border rounded-md bg-gray-50">
                        <div className="flex items-center mb-2">
                          <AlertTriangle className="h-5 w-5 text-amber-500 mr-2" />
                          <h4 className="font-medium text-gray-800">Impact Analysis</h4>
                          <span className={`ml-2 px-2 py-0.5 text-xs rounded-full font-medium 
                            ${selectedMethodImpact.impactRadius.severityLevel === 'CRITICAL' ? 'bg-red-100 text-red-800' : 
                              selectedMethodImpact.impactRadius.severityLevel === 'HIGH' ? 'bg-orange-100 text-orange-800' :
                              selectedMethodImpact.impactRadius.severityLevel === 'MEDIUM' ? 'bg-amber-100 text-amber-800' :
                              'bg-green-100 text-green-800'}`}>
                            {selectedMethodImpact.impactRadius.severityLevel}
                          </span>
                        </div>
                        
                        <p className="text-sm text-gray-600 mb-3">
                          Changes to this method will impact {selectedMethodImpact.impactRadius.totalImpact} other methods.
                        </p>
                        
                        {selectedMethodImpact.impactRadius.directlyAffected.length > 0 && (
                          <div className="mb-3">
                            <h5 className="text-sm font-medium text-gray-700 mb-1">
                              Directly Affected ({selectedMethodImpact.impactRadius.directlyAffected.length}):
                            </h5>
                            <div className="max-h-20 overflow-y-auto p-2 bg-white rounded border">
                              <ul className="text-xs space-y-1">
                                {selectedMethodImpact.impactRadius.directlyAffected.slice(0, 5).map((method, idx) => (
                                  <li key={idx} className="font-mono truncate" title={method}>
                                    {method}
                                  </li>
                                ))}
                                {selectedMethodImpact.impactRadius.directlyAffected.length > 5 && (
                                  <li className="text-gray-500">
                                    +{selectedMethodImpact.impactRadius.directlyAffected.length - 5} more...
                                  </li>
                                )}
                              </ul>
                            </div>
                          </div>
                        )}
                        
                        {selectedMethodImpact.impactRadius.indirectlyAffected.length > 0 && (
                          <div>
                            <h5 className="text-sm font-medium text-gray-700 mb-1">
                              Indirectly Affected ({selectedMethodImpact.impactRadius.indirectlyAffected.length}):
                            </h5>
                            <div className="max-h-20 overflow-y-auto p-2 bg-white rounded border">
                              <ul className="text-xs space-y-1">
                                {selectedMethodImpact.impactRadius.indirectlyAffected.slice(0, 5).map((method, idx) => (
                                  <li key={idx} className="font-mono truncate" title={method}>
                                    {method}
                                  </li>
                                ))}
                                {selectedMethodImpact.impactRadius.indirectlyAffected.length > 5 && (
                                  <li className="text-gray-500">
                                    +{selectedMethodImpact.impactRadius.indirectlyAffected.length - 5} more...
                                  </li>
                                )}
                              </ul>
                            </div>
                          </div>
                        )}
                      </div>
                    )}
                    
                    {selectedMethodData.method.calls && selectedMethodData.method.calls.length > 0 && (
                      <div className="mt-4">
                        <h4 className="font-medium text-gray-700">Method Calls ({selectedMethodData.method.calls.length})</h4>
                        <div className="mt-2 p-3 bg-gray-50 rounded-md max-h-60 overflow-y-auto">
                          <ul className="space-y-1">
                            {selectedMethodData.method.calls.map((call, index) => (
                              <li key={index} className="text-sm font-mono text-gray-700 truncate" title={call}>
                                {call}
                              </li>
                            ))}
                          </ul>
                        </div>
                      </div>
                    )}
                    
                    {relatedClass && (
                      <div className="mt-6 p-4 bg-blue-50 rounded-lg border border-blue-100">
                        <h4 className="text-blue-800 font-medium">Class Information</h4>
                        <p className="text-sm text-blue-700 mt-1">{relatedClass.fullName}</p>
                        <div className="mt-2 flex flex-wrap gap-2">
                          {relatedClass.framework && <Badge text="Inject&Rest" type="framework" />}
                          {relatedClass.test && <Badge text="TestAnnotation" type="test" />}
                          <Badge text={relatedClass.unused ? "Unused Class" : "Used Class"} type={relatedClass.unused ? "unused" : "used"} />
                        </div>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center bg-white rounded-lg shadow-md p-10 h-full">
                <Info className="h-16 w-16 text-indigo-300 mb-4" />
                <h3 className="text-xl font-medium text-gray-700">Method Details</h3>
                <p className="text-gray-500 mt-2 text-center">
                  Select a method from the list to view its details and related information.
                </p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default MethodsTab;