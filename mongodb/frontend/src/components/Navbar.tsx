import React from 'react';
import { Code2, FileCode2 } from 'lucide-react';

interface NavbarProps {
  activeTab: 'classes' | 'methods';
  setActiveTab: (tab: 'classes' | 'methods') => void;
}

const Navbar: React.FC<NavbarProps> = ({ activeTab, setActiveTab }) => {
  return (
    <header className="bg-gradient-to-r from-blue-600 to-blue-800 text-white shadow-md">
      <div className="container mx-auto px-4 py-5">
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-2">
            <Code2 size={28} className="text-white" />
            <h1 className="text-2xl font-bold">Code Analyzer</h1>
          </div>
          
          <nav className="flex space-x-1">
            <button
              onClick={() => setActiveTab('classes')}
              className={`flex items-center space-x-2 px-4 py-2 rounded-md transition-all duration-200 ${
                activeTab === 'classes'
                  ? 'bg-white text-blue-700 shadow-md'
                  : 'text-white hover:bg-blue-700'
              }`}
            >
              <FileCode2 size={18} />
              <span>Classes</span>
            </button>
            
            <button
              onClick={() => setActiveTab('methods')}
              className={`flex items-center space-x-2 px-4 py-2 rounded-md transition-all duration-200 ${
                activeTab === 'methods'
                  ? 'bg-white text-blue-700 shadow-md'
                  : 'text-white hover:bg-blue-700'
              }`}
            >
              <Code2 size={18} />
              <span>Methods</span>
            </button>
          </nav>
        </div>
      </div>
    </header>
  );
};

export default Navbar;