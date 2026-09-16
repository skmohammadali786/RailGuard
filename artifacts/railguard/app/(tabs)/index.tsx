import AsyncStorage from '@react-native-async-storage/async-storage';
import { router } from 'expo-router';
import React, { useEffect, useState } from 'react';
import RailGuardScreen from '@/components/RailGuard';

export default function HomeTab() {
  const [ready, setReady] = useState(false);
  useEffect(() => {
    AsyncStorage.getItem('railguard:session').then((session) => {
      if (session === 'active') setReady(true);
      else router.replace('/splash');
    });
  }, []);
  if (!ready) return null;
  return <RailGuardScreen screen="home" />;
}
