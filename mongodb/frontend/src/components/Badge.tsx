import React from 'react';

interface BadgeProps {
  text: string;
  type: 'framework' | 'test' | 'unused' | 'used' | 'low' | 'medium' | 'high' | 'critical';
}

const Badge: React.FC<BadgeProps> = ({ text, type }) => {
  const getColorClass = () => {
    switch (type) {
      case 'framework':
        return 'bg-purple-100 text-purple-800';
      case 'test':
        return 'bg-blue-100 text-blue-800';
      case 'unused':
        return 'bg-red-100 text-red-800';
      case 'used':
        return 'bg-green-100 text-green-800';
      case 'low':
        return 'bg-green-100 text-green-800';
      case 'medium':
        return 'bg-yellow-100 text-yellow-800';
      case 'high':
        return 'bg-orange-100 text-orange-800';
      case 'critical':
        return 'bg-red-100 text-red-800';
      default:
        return 'bg-gray-100 text-gray-800';
    }
  };

  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getColorClass()}`}>
      {text}
    </span>
  );
};

export default Badge;