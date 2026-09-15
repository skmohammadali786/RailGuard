import React from 'react';
import { Platform, StyleSheet, View } from 'react-native';
import { useColors } from '@/hooks/useColors';
import { useTheme } from '@/providers/ThemeProvider';
import { Feather } from '@expo/vector-icons';
import { BlurView } from 'expo-blur';
import { Tabs } from 'expo-router';

function ClassicTabLayout() {
  const colors = useColors();
  const { isDark } = useTheme();
  const isIOS = Platform.OS === 'ios';
  const isWeb = Platform.OS === 'web';

  return (
    <Tabs
      screenOptions={{
        tabBarActiveTintColor: colors.primary,
        tabBarInactiveTintColor: colors.mutedForeground,
        headerShown: false,
        tabBarStyle: {
          position: 'absolute',
          backgroundColor: isIOS ? 'transparent' : colors.background,
          borderTopWidth: isWeb ? 1 : 0,
          borderTopColor: colors.border,
          elevation: 0,
          ...(isWeb ? { height: 84 } : {}),
        },
        tabBarBackground: () =>
          isIOS ? (
            <BlurView intensity={100} tint={isDark ? 'dark' : 'light'} style={StyleSheet.absoluteFill} />
          ) : isWeb ? (
            <View style={[StyleSheet.absoluteFill, { backgroundColor: colors.background }]} />
          ) : null,
      }}
    >
      <Tabs.Screen name="index" options={{ title: 'Home', tabBarIcon: ({ color }) => <Feather name="home" size={22} color={color} /> }} />
      <Tabs.Screen name="inspections" options={{ title: 'Inspections', tabBarIcon: ({ color }) => <Feather name="activity" size={22} color={color} /> }} />
      <Tabs.Screen name="defects" options={{ title: 'Defects', tabBarIcon: ({ color }) => <Feather name="alert-triangle" size={22} color={color} /> }} />
      <Tabs.Screen name="map" options={{ title: 'Map', tabBarIcon: ({ color }) => <Feather name="map" size={22} color={color} /> }} />
      <Tabs.Screen name="settings" options={{ title: 'Settings', tabBarIcon: ({ color }) => <Feather name="settings" size={22} color={color} /> }} />
    </Tabs>
  );
}

export default function TabLayout() {
  return <ClassicTabLayout />;
}