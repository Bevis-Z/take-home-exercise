import React, { useState, useMemo } from 'react';
import { Filter, Info, ArrowDown, ArrowUp, Share2, AlertTriangle } from 'lucide-react';
import { Class, ImpactAnalysis, CallGraph } from '../types';
import Badge from './Badge';
import SearchBar from './SearchBar';
import DependencyList from './DependencyList';
import DependencyGraph from './DependencyGraph';

interface ClassesTabProps {
  classes: Class[];
  impactAnalysis: ImpactAnalysis[];
  callGraph: CallGraph;
}

const ClassesTab: React.FC<ClassesTabProps> = ({ 
  classes = [], 
  impactAnalysis = [],
  callGraph = { nodes: [], edges: [] }
}) => {
  const [showUnusedOnly, setShowUnusedOnly] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedClass, setSelectedClass] = useState<string | null>(null);
  const [sortField, setSortField] = useState<'name' | 'package' | 'dependencies' | 'impact'>('dependencies');
  const [sortDirection, setSortDirection] = useState<'asc' | 'desc'>('desc');
  const [view, setView] = useState<'list' | 'graph'>('list');

  const classDependencyGraph = useMemo(() => {
    const nodes = classes.map(cls => ({
      id: cls.id,
      type: 'class'
    }));

    const edges: { from: string, to: string }[] = [];
    classes.forEach(cls => {
      if (cls.dependsOn && cls.dependsOn.length > 0) {
        cls.dependsOn.forEach(dep => {
          edges.push({
            from: cls.id,
            to: dep.target
          });
        });
      }
    });

    return { nodes, edges };
  }, [classes]);

  const handleSort = (field: 'name' | 'package' | 'dependencies' | 'impact') => {
    if (sortField === field) {
      setSortDirection(sortDirection === 'asc' ? 'desc' : 'asc');
    } else {
      setSortField(field);
      setSortDirection(field === 'dependencies' || field === 'impact' ? 'desc' : 'asc');
    }
  };

  const classesWithImpact = useMemo(() => {
    if (!Array.isArray(classes) || !Array.isArray(impactAnalysis)) return [];
    
    return classes.map(cls => {
      const impact = impactAnalysis.find(impact => impact.class === cls.id);
      return {
        ...cls,
        impactTotal: impact?.impactRadius.totalImpact || 0
      };
    });
  }, [classes, impactAnalysis]);

  const filteredClasses = useMemo(() => {
    if (!Array.isArray(classesWithImpact)) return [];
    
    return classesWithImpact
      .filter(cls => !showUnusedOnly || cls.unused)
      .filter(cls => {
        if (!searchTerm) return true;
        
        const lowercaseSearchTerm = searchTerm.toLowerCase();
        
        if (cls.fullName.toLowerCase().includes(lowercaseSearchTerm)) {
          return true;
        }
        
        if (cls.simpleName.toLowerCase().includes(lowercaseSearchTerm)) {
          return true;
        }
        
        if (cls.packageName.toLowerCase().includes(lowercaseSearchTerm)) {
          return true;
        }
        
        return false;
      })
      .sort((a, b) => {
        if (sortField === 'dependencies') {
          const countA = a.dependsOn?.length || 0;
          const countB = b.dependsOn?.length || 0;
          return sortDirection === 'asc' ? countA - countB : countB - countA;
        }

        if (sortField === 'impact') {
          return sortDirection === 'asc' ? a.impactTotal - b.impactTotal : b.impactTotal - a.impactTotal;
        }
        
        const fieldA = sortField === 'name' ? a.simpleName : a.packageName;
        const fieldB = sortField === 'name' ? b.simpleName : b.packageName;
        
        if (sortDirection === 'asc') {
          return fieldA.localeCompare(fieldB);
        } else {
          return fieldB.localeCompare(fieldA);
        }
      });
  }, [classesWithImpact, showUnusedOnly, searchTerm, sortField, sortDirection]);

  const selectedClassData = useMemo(() => {
    if (!Array.isArray(classes)) return undefined;
    return classes.find(cls => cls.id === selectedClass);
  }, [classes, selectedClass]);

  const selectedClassImpact = useMemo(() => {
    if (!Array.isArray(impactAnalysis)) return undefined;
    return impactAnalysis.find(impact => impact.class === selectedClass);
  }, [impactAnalysis, selectedClass]);

  const renderSortIcon = (field: 'name' | 'package' | 'dependencies' | 'impact') => {
    if (sortField !== field) return null;
    
    return sortDirection === 'asc' ? (
      <ArrowUp className="h-4 w-4 text-gray-500" />
    ) : (
      <ArrowDown className="h-4 w-4 text-gray-500" />
    );
  };

  return (
    <div className="container mx-auto px-4 py-6">
      <div className="flex flex-col md:flex-row md:items-center md:justify-between mb-6 space-y-4 md:space-y-0">
        <h2 className="text-2xl font-bold text-gray-800">Class Dependencies</h2>
        
        <div className="flex flex-col sm:flex-row space-y-2 sm:space-y-0 sm:space-x-4">
          <SearchBar 
            searchTerm={searchTerm} 
            setSearchTerm={setSearchTerm} 
            placeholder="Search by name or package..." 
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
            <Share2 className="h-4 w-4 mr-2" />
            {view === 'list' ? 'Show Graph' : 'Show List'}
          </button>
        </div>
      </div>
      
      {view === 'graph' ? (
        <div className="bg-white rounded-lg shadow-md p-4">
          <DependencyGraph 
            callGraph={classDependencyGraph} 
            classes={classes}
            nodeType="class"
          />
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-1 bg-white rounded-lg shadow-md overflow-hidden">
            <div className="border-b px-4 py-3 bg-gray-50">
              <h3 className="font-semibold text-gray-700">Class List</h3>
              <p className="text-sm text-gray-500">
                {filteredClasses.length} of {Array.isArray(classes) ? classes.length : 0} classes
              </p>
            </div>
            
            <div className="bg-white">
              <div className="grid grid-cols-10 px-4 py-2 border-b text-sm font-medium text-gray-500">
                <div className="col-span-3 flex items-center cursor-pointer" onClick={() => handleSort('name')}>
                  <span>Class Name</span>
                  {renderSortIcon('name')}
                </div>
                <div className="col-span-3 flex items-center cursor-pointer" onClick={() => handleSort('package')}>
                  <span>Package</span>
                  {renderSortIcon('package')}
                </div>
                <div className="col-span-2 flex items-center cursor-pointer" onClick={() => handleSort('dependencies')}>
                  <span>Deps</span>
                  {renderSortIcon('dependencies')}
                </div>
                <div className="col-span-2 flex items-center cursor-pointer" onClick={() => handleSort('impact')}>
                  <span>Impact</span>
                  {renderSortIcon('impact')}
                </div>
              </div>
              
              <div className="divide-y divide-gray-100 max-h-[60vh] overflow-y-auto">
                {filteredClasses.length > 0 ? (
                  filteredClasses.map((cls) => (
                    <div
                      key={cls.id}
                      className={`grid grid-cols-10 px-4 py-3 text-sm cursor-pointer hover:bg-gray-50 transition-colors ${
                        selectedClass === cls.id ? 'bg-blue-50' : ''
                      }`}
                      onClick={() => setSelectedClass(cls.id)}
                    >
                      <div className="col-span-3 flex flex-col">
                        <div className="font-medium text-gray-800 truncate" title={cls.simpleName}>
                          {cls.simpleName}
                        </div>
                        <div className="flex mt-1 space-x-1">
                          {cls.framework && <Badge text="Inject&Rest" type="framework" />}
                          {cls.test && <Badge text="TestAnnotation" type="test" />}
                          <Badge text={cls.unused ? "Unused" : "Used"} type={cls.unused ? "unused" : "used"} />
                        </div>
                      </div>
                      <div className="col-span-3 text-gray-600 truncate" title={cls.packageName}>
                        {cls.packageName}
                      </div>
                      <div className="col-span-2 flex items-center justify-center">
                        <div className="w-20 h-10 flex items-center justify-center bg-gray-100 rounded-full px-2">
                          <span className="text-md font-mono">{cls.dependsOn?.length || 0}</span>
                        </div>
                      </div>
                      <div className="col-span-2 flex items-center justify-center">
                        <div className={`w-10 h-10 flex items-center justify-center rounded-full 
                          ${cls.impactTotal > 20 ? 'bg-red-100 text-red-800' :
                            cls.impactTotal > 10 ? 'bg-amber-100 text-amber-800' :
                            cls.impactTotal > 5 ? 'bg-yellow-100 text-yellow-800' :
                            'bg-green-100 text-green-800'}`}>
                          <span className="text-md font-mono">{cls.impactTotal}</span>
                        </div>
                      </div>
                    </div>
                  ))
                ) : (
                  <div className="px-4 py-8 text-center text-gray-500">
                    No classes found matching your criteria.
                  </div>
                )}
              </div>
            </div>
          </div>
          
          <div className="lg:col-span-2">
            {selectedClassData ? (
              <div className="space-y-6">
                <div className="bg-white rounded-lg shadow-md overflow-hidden">
                  <div className="border-b px-6 py-4 bg-gradient-to-r from-blue-500 to-blue-600">
                    <h3 className="font-bold text-xl text-white">{selectedClassData.simpleName}</h3>
                    <p className="text-blue-100 mt-1">{selectedClassData.fullName}</p>
                  </div>
                  
                  <div className="p-6">
                    <div className="flex flex-wrap gap-2 mb-4">
                      {selectedClassData.framework && <Badge text="Inject&Rest" type="framework" />}
                      {selectedClassData.test && <Badge text="TestAnnotation" type="test" />}
                      <Badge text={selectedClassData.unused ? "Unused" : "Used"} type={selectedClassData.unused ? "unused" : "used"} />
                      <div className="text-sm text-gray-600 ml-2">
                        <span className="font-medium">Dependencies:</span> {selectedClassData.dependsOn?.length || 0}
                      </div>
                      
                      {selectedClassImpact && (
                        <div className="ml-2 text-sm text-gray-600">
                          <span className="font-medium">Impact:</span> {selectedClassImpact.impactRadius.totalImpact} classes
                        </div>
                      )}
                    </div>
                    
                    <div className="mt-4">
                      <DependencyList 
                        dependencies={selectedClassData.dependsOn || []} 
                        title="Dependencies" 
                      />
                    </div>
                  </div>
                </div>
                
                {selectedClassImpact && (
                  <div className="mt-4 p-4 border rounded-md bg-gray-50">
                    <div className="flex items-center mb-2">
                      <AlertTriangle className="h-5 w-5 text-amber-500 mr-2" />
                      <h4 className="font-medium text-gray-800">Impact Analysis</h4>
                      <span className={`ml-2 px-2 py-0.5 text-xs rounded-full font-medium 
                        ${selectedClassImpact.impactRadius.severityLevel === 'CRITICAL' ? 'bg-red-100 text-red-800' : 
                          selectedClassImpact.impactRadius.severityLevel === 'HIGH' ? 'bg-orange-100 text-orange-800' :
                          selectedClassImpact.impactRadius.severityLevel === 'MEDIUM' ? 'bg-amber-100 text-amber-800' :
                          'bg-green-100 text-green-800'}`}>
                        {selectedClassImpact.impactRadius.severityLevel}
                      </span>
                    </div>
                    
                    <p className="text-sm text-gray-600 mb-3">
                      Changes to this class will impact {selectedClassImpact.impactRadius.totalImpact} other classes.
                    </p>
                    
                    {selectedClassImpact.impactRadius.directlyAffected.length > 0 && (
                      <div className="mb-3">
                        <h5 className="text-sm font-medium text-gray-700 mb-1">
                          Directly Affected ({selectedClassImpact.impactRadius.directlyAffected.length}):
                        </h5>
                        <div className="max-h-40 overflow-y-auto p-2 bg-white rounded border">
                          <ul className="text-xs space-y-1">
                            {selectedClassImpact.impactRadius.directlyAffected.map((cls, idx) => (
                              <li key={idx} className="font-mono truncate" title={cls}>
                                {cls}
                              </li>
                            ))}
                          </ul>
                        </div>
                      </div>
                    )}
                    
                    {selectedClassImpact.impactRadius.indirectlyAffected.length > 0 && (
                      <div>
                        <h5 className="text-sm font-medium text-gray-700 mb-1">
                          Indirectly Affected ({selectedClassImpact.impactRadius.indirectlyAffected.length}):
                        </h5>
                        <div className="max-h-40 overflow-y-auto p-2 bg-white rounded border">
                          <ul className="text-xs space-y-1">
                            {selectedClassImpact.impactRadius.indirectlyAffected.map((cls, idx) => (
                              <li key={idx} className="font-mono truncate" title={cls}>
                                {cls}
                              </li>
                            ))}
                          </ul>
                        </div>
                      </div>
                    )}
                  </div>
                )}
              </div>
            ) : (
              <div className="flex flex-col items-center justify-center bg-white rounded-lg shadow-md p-10 h-full">
                <Info className="h-16 w-16 text-blue-300 mb-4" />
                <h3 className="text-xl font-medium text-gray-700">Class Details</h3>
                <p className="text-gray-500 mt-2 text-center">
                  Select a class from the list to view its dependencies and impact analysis.
                </p>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
};

export default ClassesTab;