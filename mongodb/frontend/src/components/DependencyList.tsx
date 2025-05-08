import React, { useState } from 'react';
import { ChevronRight, ChevronDown } from 'lucide-react';
import Badge from './Badge';
import { Dependency } from '../types';

interface DependencyListProps {
  dependencies: Dependency[];
  title: string;
}

const DependencyList: React.FC<DependencyListProps> = ({ dependencies, title }) => {
  const [isExpanded, setIsExpanded] = useState(false);

  if (dependencies.length === 0) {
    return null;
  }

  return (
    <div className="mt-2">
      <button
        className="flex items-center space-x-2 w-full text-left"
        onClick={() => setIsExpanded(!isExpanded)}
      >
        {isExpanded ? (
          <ChevronDown className="h-4 w-4 text-gray-500" />
        ) : (
          <ChevronRight className="h-4 w-4 text-gray-500" />
        )}
        <span className="font-medium text-sm text-gray-700">{title} ({dependencies.length})</span>
      </button>
      
      {isExpanded && (
        <div className="pl-6 mt-1 space-y-1 text-sm">
          {dependencies.map((dep, index) => (
            <div key={index} className="flex items-center space-x-2">
              <span className="text-gray-700 truncate flex-1" title={dep.target}>
                {dep.target}
              </span>
              <Badge 
                text={dep.type === 'IMPORT' ? 'Import' : 'Reference'} 
                type={dep.type === 'IMPORT' ? 'framework' : 'test'} 
              />
            </div>
          ))}
        </div>
      )}
    </div>
  );
};

export default DependencyList;