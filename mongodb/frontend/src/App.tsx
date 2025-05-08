import { useState } from 'react';
import Navbar from './components/Navbar';
import ClassesTab from './components/ClassesTab';
import MethodsTab from './components/MethodsTab';
import LoadingState from './components/LoadingState';
import ErrorState from './components/ErrorState';
import { useCodeData } from './hooks/useCodeData';

function App() {
  const [activeTab, setActiveTab] = useState<'classes' | 'methods'>('classes');
  const { data, loading, error } = useCodeData();

  const renderContent = () => {
    if (loading) return <LoadingState />;
    if (error) return <ErrorState message={error} />;
    if (!data) return <ErrorState message="No data available" />;

    switch (activeTab) {
      case 'classes':
        return <ClassesTab classes={data.classes} impactAnalysis={data.impactAnalysis} callGraph={data.callGraph} />;
      case 'methods':
        return <MethodsTab classes={data.classes} methods={data.methods} callGraph={data.callGraph} impactAnalysis={data.impactAnalysis}/>;
      default:
        return <ClassesTab classes={data.classes} impactAnalysis={data.impactAnalysis} callGraph={data.callGraph} />;
    }
  };

  return (
    <div className="min-h-screen bg-gray-50">
      <Navbar activeTab={activeTab} setActiveTab={setActiveTab} />
      <main className="pt-4 pb-12">
        {renderContent()}
      </main>
    </div>
  );
}

export default App;