import { useState, useEffect } from 'react';
import { CodeData } from '../types';

export const useCodeData = () => {
  const [data, setData] = useState<CodeData | null>(null);
  const [loading, setLoading] = useState<boolean>(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const response = await fetch('/data/code-data.json');
        
        if (!response.ok) {
          throw new Error('Failed to fetch code data');
        }
        
        const jsonData = await response.json();
        setData(jsonData);
      } catch (err) {
        console.error('Error fetching code data:', err);
        setError('Failed to load code data. Please try again later.');
      } finally {
        setLoading(false);
      }
    };

    fetchData();
  }, []);

  return { data, loading, error };
};