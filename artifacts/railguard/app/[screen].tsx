import React from 'react';
import { useLocalSearchParams } from 'expo-router';
import RailGuardScreen from '@/components/RailGuard';

export default function RoutedRailGuardScreen() {
  const params = useLocalSearchParams<{ screen?: string }>();
  return <RailGuardScreen screen={params.screen ?? 'home'} />;
}