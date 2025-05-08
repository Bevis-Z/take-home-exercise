import React from 'react';
import { Loader2 } from 'lucide-react';

const LoadingState: React.FC = () => {
  return (
    <div className="flex flex-col items-center justify-center min-h-[60vh] p-8">
      <Loader2 className="h-12 w-12 text-blue-500 animate-spin mb-4" />
      <h3 className="text-xl font-medium text-gray-700">Loading code analysis data...</h3>
      <p className="text-gray-500 mt-2">Please wait while we process your codebase</p>
    </div>
  );
};

export default LoadingState;