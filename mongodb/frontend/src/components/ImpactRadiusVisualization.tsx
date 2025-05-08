import React from 'react';
import { ArrowRight } from 'lucide-react';
import Badge from './Badge';

interface ImpactRadiusVisualizationProps {
  impactRadius: {
    directlyAffected: string[];
    indirectlyAffected: string[];
    totalImpact: number;
    severityLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  };
}

const ImpactRadiusVisualization: React.FC<ImpactRadiusVisualizationProps> = ({ impactRadius }) => {
  const { directlyAffected, indirectlyAffected, totalImpact, severityLevel } = impactRadius;
  
  const getSeverityText = (level: string) => {
    switch (level.toLowerCase()) {
      case 'low':
        return 'Low Impact';
      case 'medium':
        return 'Medium Impact';
      case 'high':
        return 'High Impact';
      case 'critical':
        return 'Critical Impact';
      default:
        return 'Unknown';
    }
  };

  const renderAffectedClasses = (classes: string[], title: string) => {
    if (classes.length === 0) return null;

    return (
      <div className="mt-2">
        <h4 className="font-medium text-sm text-gray-600">{title} ({classes.length})</h4>
        <ul className="mt-1 space-y-1">
          {classes.slice(0, 5).map((className, index) => (
            <li key={index} className="text-sm text-gray-700 truncate" title={className}>
              {className}
            </li>
          ))}
          {classes.length > 5 && (
            <li className="text-sm text-blue-600 cursor-pointer hover:underline">
              + {classes.length - 5} more
            </li>
          )}
        </ul>
      </div>
    );
  };

  return (
    <div className="bg-white rounded-lg shadow p-4 border border-gray-200">
      <div className="flex items-center justify-between">
        <h3 className="font-semibold text-gray-800">Impact Analysis</h3>
        <Badge text={getSeverityText(severityLevel)} type={severityLevel.toLowerCase() as any} />
      </div>
      
      <div className="mt-3 grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="bg-gray-50 p-3 rounded-md border border-gray-100">
          <div className="font-medium text-gray-700">Total Impact</div>
          <div className="text-3xl font-bold text-blue-700 mt-1">{totalImpact}</div>
          <div className="text-xs text-gray-500">affected classes</div>
        </div>
        
        <div className="md:col-span-2">
          <div className="flex items-center space-x-2">
            <div className="w-6 h-6 rounded-full bg-blue-100 flex items-center justify-center">
              <span className="text-blue-700 text-xs font-bold">{directlyAffected.length}</span>
            </div>
            <div className="text-sm font-medium text-gray-700">Directly Affected</div>
            <ArrowRight className="h-4 w-4 text-gray-400" />
            <div className="w-6 h-6 rounded-full bg-purple-100 flex items-center justify-center">
              <span className="text-purple-700 text-xs font-bold">{indirectlyAffected.length}</span>
            </div>
            <div className="text-sm font-medium text-gray-700">Indirectly Affected</div>
          </div>
          
          {renderAffectedClasses(directlyAffected, "Directly Affected Classes")}
          {renderAffectedClasses(indirectlyAffected, "Indirectly Affected Classes")}
        </div>
      </div>
    </div>
  );
};

export default ImpactRadiusVisualization;