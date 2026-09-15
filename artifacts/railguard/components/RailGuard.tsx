import React, { useEffect, useState } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { Feather } from '@expo/vector-icons';
import { router } from 'expo-router';
import {
  Image,
  Platform,
  Pressable,
  ScrollView,
  StyleSheet,
  Switch,
  Text,
  TextInput,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { useColors } from '@/hooks/useColors';

type IconName = React.ComponentProps<typeof Feather>['name'];
type Tone = 'critical' | 'warning' | 'healthy' | 'info' | 'neutral';
export type ScreenKey = string;

const iconFor = (name: string): IconName => {
  const icons: Record<string, IconName> = {
    home: 'home', inspections: 'activity', defects: 'alert-triangle', map: 'map',
    maintenance: 'tool', reports: 'file-text', analytics: 'bar-chart-2',
    scheduling: 'calendar', settings: 'settings', notifications: 'bell',
    profile: 'user', camera: 'camera', gps: 'map-pin', alignment: 'git-merge',
    vibration: 'radio', evidence: 'paperclip', comments: 'message-square',
    security: 'shield', language: 'globe', help: 'help-circle', about: 'info',
    back: 'arrow-left', chevron: 'chevron-right', close: 'x', check: 'check',
    search: 'search', download: 'download', share: 'share-2', refresh: 'refresh-cw',
    clock: 'clock', trend: 'trending-up', user: 'user', more: 'more-horizontal',
  };
  return icons[name] ?? 'circle';
};

const toneColor = (colors: ReturnType<typeof useColors>, tone: Tone) => ({
  critical: colors.critical,
  warning: colors.warning,
  healthy: colors.healthy,
  info: colors.info,
  neutral: colors.mutedForeground,
}[tone]);

const sectionRoutes = [
  ['inspections', 'Inspections', 'activity'],
  ['defects', 'Defects', 'alert-triangle'],
  ['map', 'Railway map', 'map'],
  ['maintenance', 'Maintenance', 'tool'],
  ['reports', 'Reports', 'file-text'],
  ['analytics', 'Analytics', 'bar-chart-2'],
  ['scheduling', 'Scheduling', 'calendar'],
  ['settings', 'Settings', 'settings'],
] as const;

export const SCREEN_REGISTRY = [
  'splash', 'onboarding', 'login', 'registration', 'otp', 'email-verification', 'forgot-password', 'reset-password',
  'home', 'notifications', 'profile', 'inspection-setup', 'live-inspection', 'camera', 'detection-result', 'crack-details',
  'crack-measurement', 'object-detection', 'alignment-analysis', 'vibration-analysis', 'gps', 'inspection-summary', 'save-inspection',
  'defects', 'defect-list', 'defect-details', 'crack-history', 'image-comparison', 'growth-analysis', 'evidence', 'comments', 'engineer-verification',
  'map', 'railway-map', 'defect-map', 'track-map', 'location-details', 'risk-heatmap', 'maintenance', 'maintenance-dashboard', 'maintenance-task', 'task-details',
  'before-after', 'maintenance-verification', 'reports', 'report-details', 'evidence-package', 'pdf-preview', 'share-report',
  'analytics', 'track-health', 'health-history', 'crack-analytics', 'risk-analytics', 'maintenance-analytics', 'scheduling',
  'inspection-schedule', 'inspection-calendar', 'create-inspection', 'assigned-inspections', 'settings', 'account', 'app-settings', 'notification-settings',
  'security', 'language', 'help-center', 'about', 'screen-directory',
] as const;

const defects = [
  { id: 'CRK-2048', title: 'Gauge corner crack', section: 'North Loop · 14+320', score: '92', tone: 'critical' as Tone, time: '12 min ago', detail: 'Longitudinal surface crack at the gauge corner. AI confidence 92%. Immediate speed restriction recommended.' },
  { id: 'CRK-2044', title: 'Head check wear', section: 'North Loop · 14+108', score: '74', tone: 'warning' as Tone, time: 'Yesterday', detail: 'Progressive head check with visible polishing. Monitor growth on the next inspection cycle.' },
  { id: 'CRK-2037', title: 'Fastener shadow anomaly', section: 'South Yard · 08+902', score: '61', tone: 'warning' as Tone, time: 'Jun 17', detail: 'Possible loose fastening detected beside sleeper 08+902. Field confirmation required.' },
];

const tasks = [
  { id: 'MT-881', title: 'Replace rail clip pair', section: 'North Loop · 14+320', due: 'Due today', tone: 'critical' as Tone, assignee: 'M. Alvarez' },
  { id: 'MT-878', title: 'Grind head check', section: 'East Junction · 03+660', due: 'Due Jun 22', tone: 'warning' as Tone, assignee: 'J. Patel' },
  { id: 'MT-864', title: 'Torque verification', section: 'South Yard · 08+902', due: 'Due Jun 24', tone: 'healthy' as Tone, assignee: 'S. Morgan' },
];

function LogoMark({ small = false }: { small?: boolean }) {
  const colors = useColors();
  return (
    <View style={[styles.logoMark, { width: small ? 30 : 38, height: small ? 30 : 38, borderColor: colors.primary, backgroundColor: colors.surfaceInset }]}>
      <Image source={require('../assets/images/icon.png')} style={styles.logoImage} />
    </View>
  );
}

function IconButton({ name, onPress, label, tone = 'neutral' }: { name: string; onPress: () => void; label: string; tone?: Tone }) {
  const colors = useColors();
  return (
    <Pressable accessibilityRole="button" accessibilityLabel={label} testID={label} onPress={onPress}
      style={({ pressed }) => [styles.iconButton, { borderColor: colors.border, backgroundColor: colors.surfaceRaised, opacity: pressed ? 0.68 : 1 }]}>
      <Feather name={iconFor(name)} size={18} color={toneColor(colors, tone)} />
    </Pressable>
  );
}

function Header({ title, subtitle, icon = 'home', back = false, onBack }: { title: string; subtitle?: string; icon?: string; back?: boolean; onBack?: () => void }) {
  const colors = useColors();
  return (
    <View style={styles.header}>
      {back ? <IconButton name="back" label="Go back" onPress={onBack ?? router.back} /> : <LogoMark small />}
      <View style={styles.headerCopy}>
        <Text style={[styles.eyebrow, { color: colors.primary }]}>{icon === 'home' ? 'RAILGUARD / CONTROL' : 'RAILGUARD'}</Text>
        <Text style={[styles.headerTitle, { color: colors.foreground }]}>{title}</Text>
        {subtitle ? <Text style={[styles.headerSubtitle, { color: colors.mutedForeground }]}>{subtitle}</Text> : null}
      </View>
      {!back ? <IconButton name="notifications" label="Open notifications" onPress={() => router.push('/notifications')} /> : <View style={styles.headerSpacer} />}
    </View>
  );
}

function StatusPill({ label, tone = 'neutral' }: { label: string; tone?: Tone }) {
  const colors = useColors();
  return (
    <View style={[styles.statusPill, { backgroundColor: `${toneColor(colors, tone)}18`, borderColor: `${toneColor(colors, tone)}45` }]}>
      <View style={[styles.statusDot, { backgroundColor: toneColor(colors, tone) }]} />
      <Text style={[styles.statusText, { color: toneColor(colors, tone) }]}>{label}</Text>
    </View>
  );
}

function SectionLabel({ children, action, onAction }: { children: string; action?: string; onAction?: () => void }) {
  const colors = useColors();
  return (
    <View style={styles.sectionLabelRow}>
      <Text style={[styles.sectionLabel, { color: colors.mutedForeground }]}>{children}</Text>
      {action ? <Pressable onPress={onAction} accessibilityRole="button"><Text style={[styles.sectionAction, { color: colors.primary }]}>{action}</Text></Pressable> : null}
    </View>
  );
}

function MetricTile({ value, label, tone, onPress }: { value: string; label: string; tone: Tone; onPress?: () => void }) {
  const colors = useColors();
  return (
    <Pressable onPress={onPress} disabled={!onPress} style={({ pressed }) => [styles.metricTile, { backgroundColor: colors.card, borderColor: colors.border, opacity: pressed ? 0.72 : 1 }]}>
      <Text style={[styles.metricValue, { color: toneColor(colors, tone) }]}>{value}</Text>
      <Text style={[styles.metricLabel, { color: colors.mutedForeground }]}>{label}</Text>
    </Pressable>
  );
}

function RailCard({ children, onPress, style }: { children: React.ReactNode; onPress?: () => void; style?: object }) {
  const colors = useColors();
  return <Pressable onPress={onPress} disabled={!onPress} style={({ pressed }) => [styles.card, { backgroundColor: colors.card, borderColor: colors.border, opacity: pressed ? 0.76 : 1 }, style]}>{children}</Pressable>;
}

function ListRow({ icon, title, subtitle, trailing, tone = 'neutral', onPress }: { icon: string; title: string; subtitle: string; trailing?: string; tone?: Tone; onPress?: () => void }) {
  const colors = useColors();
  return (
    <RailCard onPress={onPress} style={styles.listRow}>
      <View style={[styles.rowIcon, { backgroundColor: `${toneColor(colors, tone)}16` }]}><Feather name={iconFor(icon)} size={17} color={toneColor(colors, tone)} /></View>
      <View style={styles.rowMain}><Text style={[styles.rowTitle, { color: colors.foreground }]}>{title}</Text><Text style={[styles.rowSubtitle, { color: colors.mutedForeground }]}>{subtitle}</Text></View>
      {trailing ? <Text style={[styles.rowTrailing, { color: toneColor(colors, tone) }]}>{trailing}</Text> : null}
      {onPress ? <Feather name="chevron-right" size={16} color={colors.mutedForeground} /> : null}
    </RailCard>
  );
}

function PrimaryButton({ title, icon = 'arrow-right', onPress, secondary = false, disabled = false }: { title: string; icon?: string; onPress: () => void; secondary?: boolean; disabled?: boolean }) {
  const colors = useColors();
  return (
    <Pressable accessibilityRole="button" testID={title} disabled={disabled} onPress={onPress}
      style={({ pressed }) => [styles.primaryButton, { backgroundColor: secondary ? colors.secondary : colors.primary, borderColor: secondary ? colors.border : colors.primary, opacity: disabled ? 0.4 : pressed ? 0.74 : 1 }]}>
      <Text style={[styles.primaryButtonText, { color: secondary ? colors.foreground : colors.primaryForeground }]}>{title}</Text>
      <Feather name={iconFor(icon)} size={17} color={secondary ? colors.foreground : colors.primaryForeground} />
    </Pressable>
  );
}

function EmptyState({ icon, title, body, action, onAction }: { icon: string; title: string; body: string; action?: string; onAction?: () => void }) {
  const colors = useColors();
  return (
    <View style={[styles.emptyState, { borderColor: colors.border, backgroundColor: colors.surfaceInset }]}>
      <View style={[styles.emptyIcon, { backgroundColor: colors.secondary }]}><Feather name={iconFor(icon)} size={22} color={colors.primary} /></View>
      <Text style={[styles.emptyTitle, { color: colors.foreground }]}>{title}</Text>
      <Text style={[styles.emptyBody, { color: colors.mutedForeground }]}>{body}</Text>
      {action && onAction ? <PrimaryButton title={action} onPress={onAction} /> : null}
    </View>
  );
}

function HomeScreen() {
  const colors = useColors();
  return (
    <>
      <Header title="Field overview" subtitle="Tuesday, 18 June 2024 · Shift A" />
      <SectionLabel action="View profile" onAction={() => router.push('/profile')}>SHIFT STATUS</SectionLabel>
      <RailCard style={styles.shiftCard}>
        <View style={styles.shiftTop}><View><Text style={[styles.shiftTitle, { color: colors.foreground }]}>North corridor patrol</Text><Text style={[styles.shiftMeta, { color: colors.mutedForeground }]}>E. Chen · Unit 04 · 06:00—14:00</Text></View><StatusPill label="On duty" tone="healthy" /></View>
        <View style={[styles.progressTrack, { backgroundColor: colors.secondary }]}><View style={[styles.progressFill, { backgroundColor: colors.healthy, width: '61%' }]} /></View>
        <View style={styles.shiftBottom}><Text style={[styles.shiftMeta, { color: colors.mutedForeground }]}>Coverage progress</Text><Text style={[styles.shiftPercent, { color: colors.healthy }]}>61%</Text></View>
      </RailCard>
      <SectionLabel action="Open defects" onAction={() => router.push('/defects')}>NETWORK PULSE</SectionLabel>
      <View style={styles.metricGrid}>
        <MetricTile value="18" label="Open defects" tone="critical" onPress={() => router.push('/defects')} />
        <MetricTile value="06" label="Due today" tone="warning" onPress={() => router.push('/maintenance')} />
        <MetricTile value="94.2%" label="Track health" tone="healthy" onPress={() => router.push('/track-health')} />
      </View>
      <SectionLabel action="Start new" onAction={() => router.push('/inspection-setup')}>ACTIVE INSPECTION</SectionLabel>
      <RailCard onPress={() => router.push('/live-inspection')} style={styles.activeInspection}>
        <View style={[styles.activeIcon, { backgroundColor: colors.surfaceInset }]}><Feather name="radio" size={20} color={colors.primary} /></View>
        <View style={styles.rowMain}><Text style={[styles.rowTitle, { color: colors.foreground }]}>North Loop · Section 14</Text><Text style={[styles.rowSubtitle, { color: colors.mutedForeground }]}>Last scan 8 min ago · 1.8 km remaining</Text></View>
        <Feather name="arrow-up-right" size={18} color={colors.primary} />
      </RailCard>
      <SectionLabel action="See all" onAction={() => router.push('/notifications')}>REQUIRES ATTENTION</SectionLabel>
      <ListRow icon="alert-triangle" title="CRK-2048 · Gauge corner crack" subtitle="North Loop · 14+320 · Detected 12 min ago" trailing="92" tone="critical" onPress={() => router.push('/crack-details')} />
      <ListRow icon="tool" title="MT-881 · Replace rail clip pair" subtitle="North Loop · Assigned to M. Alvarez" trailing="DUE" tone="warning" onPress={() => router.push('/task-details')} />
      <SectionLabel>WORKSPACE</SectionLabel>
      <View style={styles.workspaceGrid}>{sectionRoutes.map(([key, label, icon]) => <Pressable key={key} onPress={() => router.push(`/${key}`)} style={({ pressed }) => [styles.workspaceItem, { borderColor: colors.border, backgroundColor: colors.card, opacity: pressed ? 0.7 : 1 }]}><Feather name={iconFor(icon)} size={17} color={colors.primary} /><Text style={[styles.workspaceLabel, { color: colors.foreground }]}>{label}</Text></Pressable>)}</View>
      <PrimaryButton title="Browse all RailGuard screens" icon="list" secondary onPress={() => router.push('/screen-directory')} />
    </>
  );
}

function InspectionsScreen() {
  const colors = useColors();
  return <><Header title="Inspections" subtitle="Field runs and saved evidence" /><PrimaryButton title="Start inspection" icon="play" onPress={() => router.push('/inspection-setup')} /><SectionLabel action="Calendar" onAction={() => router.push('/inspection-calendar')}>TODAY · 3 RUNS</SectionLabel><ListRow icon="radio" title="North Loop · Section 14" subtitle="In progress · E. Chen · Started 08:42" trailing="LIVE" tone="healthy" onPress={() => router.push('/live-inspection')} /><ListRow icon="check-circle" title="East Junction · Section 03" subtitle="Completed 07:15 · 1.2 km surveyed" trailing="SAVED" tone="healthy" onPress={() => router.push('/inspection-summary')} /><ListRow icon="clock" title="South Yard · Section 08" subtitle="Assigned to S. Morgan · 13:30" trailing="NEXT" tone="info" onPress={() => router.push('/assigned-inspections')} /><SectionLabel>RECENT EVIDENCE</SectionLabel><RailCard onPress={() => router.push('/evidence')}><Text style={[styles.cardTitle, { color: colors.foreground }]}>Evidence package · INSP-240618-04</Text><Text style={[styles.cardBody, { color: colors.mutedForeground }]}>42 frames · GPS trace · 3 detections · Synced locally</Text></RailCard></>;
}

function DefectsScreen({ variant = 'defects' }: { variant?: 'defects' | 'defect-list' }) {
  const isList = variant === 'defect-list';
  return <><Header title={isList ? 'Defect list' : 'Defects'} subtitle={isList ? 'Complete register · filterable findings' : 'AI detections requiring review'} /><View style={styles.filterRow}><StatusPill label="18 open" tone="critical" /><StatusPill label="5 new today" tone="info" /><StatusPill label="3 verified" tone="healthy" /></View>{defects.map((item) => <ListRow key={item.id} icon="alert-triangle" title={`${item.id} · ${item.title}`} subtitle={`${item.section} · ${item.time}`} trailing={item.score} tone={item.tone} onPress={() => router.push('/defect-details')} />)}<EmptyState icon="search" title="Search the full defect register" body="Filter by section, severity, or detection source to find an earlier record." action="Open crack history" onAction={() => router.push('/crack-history')} /></>;
}

function MapScreen({ variant = 'map' }: { variant?: 'map' | 'railway-map' }) {
  const colors = useColors();
  const isRailway = variant === 'railway-map';
  return <><Header title={isRailway ? 'Railway map' : 'Map'} subtitle={isRailway ? 'Network condition at a glance' : 'Live network view · 28.4 km monitored'} /><RailCard style={styles.mapCard}><View style={[styles.mapGrid, { backgroundColor: colors.surfaceInset }]}>{Array.from({ length: 10 }).map((_, i) => <View key={i} style={[styles.mapLine, { backgroundColor: i === 4 ? colors.primary : colors.border, transform: [{ rotate: i % 2 ? '8deg' : '-8deg' }] }]} />)}{defects.map((item, i) => <View key={item.id} style={[styles.mapPin, { left: `${25 + i * 24}%`, top: `${36 + (i % 2) * 22}%`, backgroundColor: toneColor(colors, item.tone) }]}><Feather name="alert-triangle" size={11} color={colors.background} /></View>)}</View><View style={styles.mapLegend}><StatusPill label="Critical" tone="critical" /><StatusPill label="Warning" tone="warning" /><StatusPill label="Healthy" tone="healthy" /></View></RailCard><SectionLabel action="Open defect map" onAction={() => router.push('/defect-map')}>NETWORK SECTIONS</SectionLabel><ListRow icon="map-pin" title="North Loop · 14+000—15+250" subtitle="2 critical · 4 warnings · 94% healthy" trailing="HIGH" tone="critical" onPress={() => router.push('/location-details')} /><ListRow icon="map-pin" title="East Junction · 03+000—04+200" subtitle="0 critical · 2 warnings · 98% healthy" trailing="LOW" tone="healthy" onPress={() => router.push('/location-details')} /><ListRow icon="map-pin" title="South Yard · 08+000—09+600" subtitle="1 warning · 96% healthy" trailing="MED" tone="warning" onPress={() => router.push('/location-details')} /></>;
}

function MaintenanceScreen({ variant = 'maintenance' }: { variant?: 'maintenance' | 'maintenance-dashboard' }) {
  const isDashboard = variant === 'maintenance-dashboard';
  return <><Header title={isDashboard ? 'Maintenance dashboard' : 'Maintenance'} subtitle={isDashboard ? 'Open work, response time, and verification' : 'Open work and verification'} /><View style={styles.metricGrid}><MetricTile value="06" label="Due today" tone="warning" /><MetricTile value="14" label="Open tasks" tone="info" /><MetricTile value="91%" label="On schedule" tone="healthy" /></View><PrimaryButton title="Create maintenance task" icon="plus" onPress={() => router.push('/maintenance-task')} /><SectionLabel action="View all" onAction={() => router.push('/task-details')}>PRIORITY QUEUE</SectionLabel>{tasks.map((task) => <ListRow key={task.id} icon="tool" title={`${task.id} · ${task.title}`} subtitle={`${task.section} · ${task.assignee}`} trailing={task.due.replace('Due ', '')} tone={task.tone} onPress={() => router.push('/task-details')} />)}<PrimaryButton title="Open maintenance analytics" icon="bar-chart-2" secondary onPress={() => router.push('/maintenance-analytics')} /></>;
}

function ReportsScreen() {
  const colors = useColors();
  return <><Header title="Reports" subtitle="Issued inspection intelligence" /><PrimaryButton title="Build evidence package" icon="package" onPress={() => router.push('/evidence-package')} /><SectionLabel>RECENT REPORTS</SectionLabel><ListRow icon="file-text" title="North corridor · Weekly safety report" subtitle="Issued Jun 17 · 18 pages · 12 defects" trailing="PDF" tone="info" onPress={() => router.push('/report-details')} /><ListRow icon="file-text" title="East Junction · Inspection 240616" subtitle="Issued Jun 16 · 8 pages · No critical findings" trailing="PDF" tone="healthy" onPress={() => router.push('/pdf-preview')} /><RailCard style={styles.reportQuote}><Feather name="message-square" size={18} color={colors.primary} /><Text style={[styles.cardBody, { color: colors.foreground }]}>“North Loop remains operable with a temporary 25 km/h restriction at 14+320 until MT-881 is verified.”</Text><Text style={[styles.rowSubtitle, { color: colors.mutedForeground }]}>AI safety summary · confidence 88%</Text></RailCard></>;
}

function AnalyticsScreen() {
  const colors = useColors();
  return <><Header title="Analytics" subtitle="Health signals over time" /><RailCard style={styles.healthCard}><View style={styles.healthTop}><View><Text style={[styles.eyebrow, { color: colors.mutedForeground }]}>NETWORK HEALTH</Text><Text style={[styles.healthValue, { color: colors.healthy }]}>94.2%</Text></View><StatusPill label="↑ 1.8% this month" tone="healthy" /></View><View style={styles.chart}>{[55, 62, 58, 70, 68, 77, 82, 78, 92].map((height, index) => <View key={index} style={styles.barWrap}><View style={[styles.bar, { height: `${height}%`, backgroundColor: index === 8 ? colors.primary : colors.healthy }]} /></View>)}</View><View style={styles.chartLabels}><Text style={[styles.rowSubtitle, { color: colors.mutedForeground }]}>APR</Text><Text style={[styles.rowSubtitle, { color: colors.mutedForeground }]}>MAY</Text><Text style={[styles.rowSubtitle, { color: colors.mutedForeground }]}>JUN</Text></View></RailCard><SectionLabel>ANALYSIS MODULES</SectionLabel><ListRow icon="trending-up" title="Track health" subtitle="Condition score by corridor" tone="healthy" onPress={() => router.push('/track-health')} /><ListRow icon="activity" title="Crack analytics" subtitle="Growth, density, and severity" tone="critical" onPress={() => router.push('/crack-analytics')} /><ListRow icon="bar-chart-2" title="Maintenance analytics" subtitle="Response time and completion" tone="info" onPress={() => router.push('/maintenance-analytics')} /></>;
}

function SchedulingScreen({ variant = 'scheduling' }: { variant?: 'scheduling' | 'inspection-schedule' }) {
  const isSchedule = variant === 'inspection-schedule';
  return <><Header title={isSchedule ? 'Inspection schedule' : 'Scheduling'} subtitle={isSchedule ? 'Planned field coverage by section' : 'Planned field coverage'} /><PrimaryButton title="Create inspection" icon="plus" onPress={() => router.push('/create-inspection')} /><SectionLabel action="Calendar view" onAction={() => router.push('/inspection-calendar')}>UPCOMING</SectionLabel><ListRow icon="calendar" title="North Loop · Section 14" subtitle="Today · 08:30 · E. Chen" trailing="TODAY" tone="healthy" onPress={() => router.push('/assigned-inspections')} /><ListRow icon="calendar" title="South Yard · Section 08" subtitle="Tomorrow · 13:30 · S. Morgan" trailing="NEXT" tone="info" onPress={() => router.push('/assigned-inspections')} /><ListRow icon="calendar" title="West Cut · Section 22" subtitle="Jun 21 · 09:00 · Unassigned" trailing="OPEN" tone="warning" onPress={() => router.push('/create-inspection')} /><EmptyState icon="calendar" title="Keep the corridor covered" body="Schedule a run when a section falls outside its inspection interval." action="Open inspection calendar" onAction={() => router.push('/inspection-calendar')} /></>;
}

function SettingsScreen() {
  return <><Header title="Settings" subtitle="Workspace and device controls" /><SectionLabel>ACCOUNT</SectionLabel><ListRow icon="user" title="Account" subtitle="E. Chen · Rail safety lead" onPress={() => router.push('/account')} /><ListRow icon="shield" title="Security" subtitle="Passcode, sessions, and access" onPress={() => router.push('/security')} /><SectionLabel>APP</SectionLabel><ListRow icon="settings" title="App settings" subtitle="Units, capture, and field mode" onPress={() => router.push('/app-settings')} /><ListRow icon="bell" title="Notification settings" subtitle="Alerts and shift summaries" onPress={() => router.push('/notification-settings')} /><ListRow icon="globe" title="Language" subtitle="English (United States)" onPress={() => router.push('/language')} /><SectionLabel>SUPPORT</SectionLabel><ListRow icon="help-circle" title="Help center" subtitle="Guides for field teams" onPress={() => router.push('/help-center')} /><ListRow icon="info" title="About RailGuard" subtitle="Version 1.0.0 · Build 240618" onPress={() => router.push('/about')} /></>;
}

function FormScreen({ kind }: { kind: string }) {
  const colors = useColors();
  const [value, setValue] = useState('');
  const isAuth = ['login', 'registration', 'forgot-password', 'reset-password'].includes(kind);
  const titles: Record<string, string> = { splash: 'RailGuard', login: 'Welcome back', registration: 'Create your field account', 'forgot-password': 'Recover access', 'reset-password': 'Set a new passcode', otp: 'Verify your number', 'email-verification': 'Verify your email', onboarding: 'Built for the live track' };
  const title = titles[kind] ?? 'RailGuard';
  const submit = () => {
    if (kind === 'login' || kind === 'registration') router.replace('/');
    else if (kind === 'forgot-password') router.push('/otp');
    else if (kind === 'otp' || kind === 'email-verification') router.replace('/');
    else if (kind === 'reset-password') router.push('/login');
  };
  return <View style={styles.authWrap}><View style={styles.authBrand}><LogoMark /><Text style={[styles.brandName, { color: colors.foreground }]}>RAILGUARD</Text><Text style={[styles.brandTag, { color: colors.mutedForeground }]}>FIELD ENGINEERING CONTROL</Text></View><Text style={[styles.authTitle, { color: colors.foreground }]}>{title}</Text><Text style={[styles.authBody, { color: colors.mutedForeground }]}>{kind === 'splash' ? 'A trusted instrument for the live track.' : kind === 'onboarding' ? 'A calm, precise instrument for detecting rail defects before they become incidents.' : 'Secure access to live inspections, evidence, and network risk.'}</Text>{kind === 'splash' ? <PrimaryButton title="Continue to sign in" onPress={() => router.replace('/login')} /> : kind === 'onboarding' ? <><View style={styles.onboardingRow}><View style={[styles.onboardingIcon, { backgroundColor: colors.secondary }]}><Feather name="camera" size={19} color={colors.primary} /></View><View style={styles.rowMain}><Text style={[styles.rowTitle, { color: colors.foreground }]}>Capture once, review anywhere</Text><Text style={[styles.rowSubtitle, { color: colors.mutedForeground }]}>AI findings stay linked to the exact track position.</Text></View></View><View style={styles.onboardingRow}><View style={[styles.onboardingIcon, { backgroundColor: colors.secondary }]}><Feather name="shield" size={19} color={colors.healthy} /></View><View style={styles.rowMain}><Text style={[styles.rowTitle, { color: colors.foreground }]}>Evidence that stands up</Text><Text style={[styles.rowSubtitle, { color: colors.mutedForeground }]}>A defensible record for every decision.</Text></View></View><PrimaryButton title="Enter RailGuard" onPress={() => router.replace('/login')} /></> : <><TextInput value={value} onChangeText={setValue} placeholder={kind === 'login' ? 'Work email' : kind === 'otp' ? '6-digit verification code' : 'Email address'} placeholderTextColor={colors.mutedForeground} keyboardType={kind === 'otp' ? 'number-pad' : 'email-address'} style={[styles.input, { color: colors.foreground, backgroundColor: colors.input, borderColor: colors.border }]} /><TextInput secureTextEntry={kind !== 'otp' && kind !== 'forgot-password' && kind !== 'email-verification'} placeholder={kind === 'login' ? 'Password' : kind === 'reset-password' ? 'New password' : 'Full name'} placeholderTextColor={colors.mutedForeground} style={[styles.input, { color: colors.foreground, backgroundColor: colors.input, borderColor: colors.border }]} /><PrimaryButton title={kind === 'login' ? 'Sign in' : kind === 'registration' ? 'Create account' : 'Continue'} onPress={submit} />{kind === 'login' ? <Pressable onPress={() => router.push('/forgot-password')}><Text style={[styles.authLink, { color: colors.primary }]}>Forgot password?</Text></Pressable> : null}{kind === 'login' ? <Pressable onPress={() => router.push('/registration')}><Text style={[styles.authLink, { color: colors.primary }]}>Create a new account</Text></Pressable> : null}</>}</View>;
}

function InspectionSetup() {
  const colors = useColors();
  return <><Header title="Inspection setup" subtitle="Configure the next field run" back /><SectionLabel>ROUTE</SectionLabel><RailCard><Text style={[styles.cardTitle, { color: colors.foreground }]}>North Loop · Section 14</Text><Text style={[styles.cardBody, { color: colors.mutedForeground }]}>14+000 → 15+250 · 1.25 km · Main line</Text><View style={styles.inlineMeta}><StatusPill label="High priority" tone="critical" /><Text style={[styles.rowSubtitle, { color: colors.mutedForeground }]}>Last inspected 6 days ago</Text></View></RailCard><SectionLabel>CAPTURE PROFILE</SectionLabel><ListRow icon="camera" title="Visual + thermal capture" subtitle="Front camera · 4K · 30 fps" trailing="ON" tone="info" /><ListRow icon="gps" title="GPS trace" subtitle="High accuracy · automatic" trailing="ON" tone="healthy" /><ListRow icon="vibration" title="Vibration analysis" subtitle="Phone sensor · live sampling" trailing="ON" tone="healthy" /><PrimaryButton title="Begin live inspection" icon="play" onPress={() => router.push('/live-inspection')} /></>;
}

function LiveInspection() {
  const colors = useColors();
  return <><Header title="Live inspection" subtitle="North Loop · 14+000—15+250" back /><View style={[styles.scanFrame, { borderColor: colors.primary, backgroundColor: colors.surfaceInset }]}><View style={[styles.scanCorner, styles.cornerTL, { borderColor: colors.primary }]} /><View style={[styles.scanCorner, styles.cornerTR, { borderColor: colors.primary }]} /><View style={[styles.scanCorner, styles.cornerBL, { borderColor: colors.primary }]} /><View style={[styles.scanCorner, styles.cornerBR, { borderColor: colors.primary }]} /><View style={styles.scanRail}><View style={[styles.railLine, { backgroundColor: colors.border }]} /><View style={[styles.railLine, { backgroundColor: colors.primary }]} /></View><View style={styles.scanBadge}><View style={[styles.statusDot, { backgroundColor: colors.healthy }]} /><Text style={[styles.statusText, { color: colors.healthy }]}>ANALYSIS LIVE</Text></View></View><View style={styles.liveStats}><MetricTile value="00:18:42" label="Elapsed" tone="info" /><MetricTile value="0.82 km" label="Coverage" tone="healthy" /><MetricTile value="94.1%" label="Signal" tone="healthy" /></View><PrimaryButton title="Review latest detection" icon="arrow-right" onPress={() => router.push('/detection-result')} /><View style={styles.buttonRow}><PrimaryButton title="Camera" icon="camera" secondary onPress={() => router.push('/camera')} /><PrimaryButton title="GPS" icon="map-pin" secondary onPress={() => router.push('/gps')} /></View></>;
}

function DetectionResult() {
  const colors = useColors();
  return <><Header title="Detection result" subtitle="Captured 08:54:16 · AI review complete" back /><RailCard style={styles.detectionHero}><View style={[styles.detectionImage, { backgroundColor: colors.surfaceInset }]}><Feather name="maximize" size={22} color={colors.primary} /><View style={[styles.detectionMark, { borderColor: colors.critical }]} /></View><View style={styles.detectionOverlay}><StatusPill label="Critical finding" tone="critical" /><Text style={[styles.detectionConfidence, { color: colors.foreground }]}>92% confidence</Text></View></RailCard><SectionLabel>AI ASSESSMENT</SectionLabel><RailCard><Text style={[styles.cardTitle, { color: colors.foreground }]}>Gauge corner crack</Text><Text style={[styles.cardBody, { color: colors.mutedForeground }]}>Longitudinal surface indication at 14+320. Pattern matches rolling contact fatigue.</Text><View style={[styles.detailGrid, { borderTopColor: colors.border }]}><View><Text style={[styles.detailLabel, { color: colors.mutedForeground }]}>EST. LENGTH</Text><Text style={[styles.detailValue, { color: colors.foreground }]}>46 mm</Text></View><View><Text style={[styles.detailLabel, { color: colors.mutedForeground }]}>TRACK SIDE</Text><Text style={[styles.detailValue, { color: colors.foreground }]}>Up line</Text></View><View><Text style={[styles.detailLabel, { color: colors.mutedForeground }]}>RISK SCORE</Text><Text style={[styles.detailValue, { color: colors.critical }]}>92 / 100</Text></View></View></RailCard><PrimaryButton title="Open crack details" onPress={() => router.push('/crack-details')} /><PrimaryButton title="Measure manually" icon="crosshair" secondary onPress={() => router.push('/crack-measurement')} /></>;
}

function CrackDetails() {
  const colors = useColors();
  return <><Header title="CRK-2048" subtitle="Gauge corner crack · North Loop 14+320" back /><View style={styles.detailHeader}><StatusPill label="Critical · unverified" tone="critical" /><Text style={[styles.detailScore, { color: colors.critical }]}>92</Text><Text style={[styles.rowSubtitle, { color: colors.mutedForeground }]}>RISK SCORE / 100</Text></View><SectionLabel>FINDING</SectionLabel><RailCard><Text style={[styles.cardTitle, { color: colors.foreground }]}>Immediate review required</Text><Text style={[styles.cardBody, { color: colors.mutedForeground }]}>AI recommends a temporary 25 km/h restriction until a qualified engineer verifies the finding and maintenance task MT-881 is complete.</Text><View style={styles.inlineMeta}><StatusPill label="46 mm estimated" tone="warning" /><StatusPill label="Up line" tone="info" /></View></RailCard><SectionLabel>ACTIONS</SectionLabel><PrimaryButton title="Measure crack" icon="crosshair" onPress={() => router.push('/crack-measurement')} /><PrimaryButton title="Verify as engineer" icon="check" secondary onPress={() => router.push('/engineer-verification')} /><ListRow icon="tool" title="MT-881 · Replace rail clip pair" subtitle="Assigned to M. Alvarez · due today" trailing="OPEN" tone="warning" onPress={() => router.push('/task-details')} /><SectionLabel>TRACEABILITY</SectionLabel><ListRow icon="camera" title="Original capture" subtitle="08:54:16 · Unit 04 · frame 00482" onPress={() => router.push('/image-comparison')} /><ListRow icon="message-square" title="Comments" subtitle="2 engineer notes" onPress={() => router.push('/comments')} /></>;
}

function DetailScreen({ kind }: { kind: string }) {
  const colors = useColors();
  const config: Record<string, { title: string; subtitle: string; icon: string; body: string; action?: string; next?: string }> = {
    'crack-measurement': { title: 'Crack measurement', subtitle: 'CRK-2048 · field confirmation', icon: 'crosshair', body: 'Use the calibration marker to confirm the AI estimate. The measurement is stored with the original frame.', action: 'Save measurement', next: '/crack-details' },
    camera: { title: 'Camera capture', subtitle: 'Visual evidence · front camera', icon: 'camera', body: 'Camera is ready for a manual frame. Keep the rail head inside the guide and capture a sharp, perpendicular view.', action: 'Capture frame', next: '/detection-result' },
    'object-detection': { title: 'Object detection', subtitle: 'Fasteners, sleepers, and rail profile', icon: 'search', body: 'Object pass found 18 fasteners, 2 sleepers, and one anomaly near the gauge corner.', action: 'Review anomaly', next: '/crack-details' },
    'defect-details': { title: 'Defect details', subtitle: 'CRK-2048 · review record', icon: 'alert-triangle', body: 'A complete finding record with source frame, confidence, location, measurement, and current operational restriction.', action: 'Open crack details', next: '/crack-details' },
    'crack-history': { title: 'Crack history', subtitle: 'North corridor · observations', icon: 'clock', body: 'CRK-2048 was first observed at 31 mm on Jun 12 and measured at 46 mm today. Growth is above the watch threshold.', action: 'Compare images', next: '/image-comparison' },
    'alignment-analysis': { title: 'Alignment analysis', subtitle: 'North Loop · live geometry', icon: 'alignment', body: 'Horizontal alignment is within tolerance for the current segment. One short transition needs a follow-up pass.', action: 'View section', next: '/location-details' },
    'vibration-analysis': { title: 'Vibration analysis', subtitle: 'Live sensor sampling', icon: 'vibration', body: 'Peak acceleration is 0.34 g. No harmonic signature associated with a loose fastening was found.', action: 'Save sensor trace', next: '/inspection-summary' },
    gps: { title: 'GPS position', subtitle: 'High accuracy trace', icon: 'gps', body: '14+320 · 40.7128° N, 74.0060° W · ±3 m accuracy. Position is linked to the active frame.', action: 'Continue inspection', next: '/live-inspection' },
    'inspection-summary': { title: 'Inspection summary', subtitle: 'North Loop · completed 09:16', icon: 'check-circle', body: '1.25 km surveyed · 42 frames · 3 findings · 1 critical. Review the evidence package before saving.', action: 'Save inspection', next: '/save-inspection' },
    'save-inspection': { title: 'Save inspection', subtitle: 'INSP-240618-04 · ready to sync', icon: 'download', body: 'All frames, sensor traces, GPS points, and findings are bundled locally. You can sync when connectivity returns.', action: 'Save to device', next: '/' },
    'image-comparison': { title: 'Image comparison', subtitle: 'CRK-2048 · progression review', icon: 'copy', body: 'Current capture shows a 46 mm indication. The previous frame from Jun 12 measured 31 mm; engineer confirmation is required.', action: 'Open growth analysis', next: '/growth-analysis' },
    'growth-analysis': { title: 'Growth analysis', subtitle: 'CRK-2048 · four observation points', icon: 'trending-up', body: 'Estimated growth is 3.7 mm per inspection cycle. This exceeds the local watch threshold of 2 mm.', action: 'Create maintenance task', next: '/maintenance-task' },
    evidence: { title: 'Evidence', subtitle: 'INSP-240618-04 · 42 frames', icon: 'evidence', body: 'Every capture is timestamped, geolocated, and linked to the inspection record. Add a note or open the evidence package.', action: 'Open comments', next: '/comments' },
    comments: { title: 'Comments', subtitle: 'CRK-2048 · engineer notes', icon: 'comments', body: 'M. Alvarez · “Confirm with gauge corner profile before releasing restriction.”  S. Morgan · “Maintenance slot held for 14:00.”', action: 'Verify finding', next: '/engineer-verification' },
    'engineer-verification': { title: 'Engineer verification', subtitle: 'CRK-2048 · sign-off required', icon: 'check', body: 'Review the AI evidence and confirm whether the finding is actionable. Your sign-off is written into the audit trail.', action: 'Sign and verify', next: '/inspection-summary' },
    'defect-map': { title: 'Defect map', subtitle: '18 open findings · live network', icon: 'map', body: 'Critical findings cluster around North Loop 14+320. Tap a section to inspect location-specific risk.', action: 'Open location details', next: '/location-details' },
    'track-map': { title: 'Track map', subtitle: 'North corridor · 28.4 km', icon: 'map', body: 'The corridor is divided into 22 monitored sections. Current position is 14+320 on the up line.', action: 'Open current location', next: '/location-details' },
    'location-details': { title: 'Location details', subtitle: 'North Loop · 14+320', icon: 'gps', body: 'Up line · rail 60E1 · last tamp Jun 04. One critical defect and two warnings are within 250 m.', action: 'Open risk heatmap', next: '/risk-heatmap' },
    'risk-heatmap': { title: 'Risk heatmap', subtitle: 'Network risk by section', icon: 'alert-triangle', body: 'North Loop is the only section above the intervention threshold. Restriction remains active until MT-881 is verified.', action: 'Open maintenance task', next: '/task-details' },
    'maintenance-task': { title: 'Maintenance task', subtitle: 'Create task from CRK-2048', icon: 'tool', body: 'Draft task: replace rail clip pair at 14+320, then capture verification evidence. Assign a qualified engineer and due window.', action: 'Create task', next: '/task-details' },
    'task-details': { title: 'MT-881', subtitle: 'Replace rail clip pair · North Loop 14+320', icon: 'tool', body: 'Assigned to M. Alvarez. Work window today 14:00—15:00. Safety restriction remains active until verification.', action: 'Open before / after', next: '/before-after' },
    'before-after': { title: 'Before / after', subtitle: 'MT-881 · work evidence', icon: 'copy', body: 'Before: loose clip visible beside sleeper 14+320. After: replacement clip seated and torqued to 220 Nm.', action: 'Verify maintenance', next: '/maintenance-verification' },
    'maintenance-verification': { title: 'Maintenance verification', subtitle: 'MT-881 · engineer sign-off', icon: 'check', body: 'Confirm the work is complete, the evidence is clear, and the temporary restriction can be released.', action: 'Verify completed work', next: '/maintenance' },
    'report-details': { title: 'Weekly safety report', subtitle: 'North corridor · issued Jun 17', icon: 'reports', body: '18 pages · 12 findings · 3 maintenance actions. AI summary and source evidence are included.', action: 'Preview PDF', next: '/pdf-preview' },
    'evidence-package': { title: 'Evidence package', subtitle: 'INSP-240618-04 · assembling locally', icon: 'evidence', body: '42 frames, GPS trace, sensor sample, and 3 findings are ready to package. No network connection is required.', action: 'Preview package', next: '/pdf-preview' },
    'pdf-preview': { title: 'PDF preview', subtitle: 'North corridor · safety report', icon: 'file-text', body: 'RailGuard safety report · issued 18 June 2024. This preview includes the executive summary, finding register, and signed evidence index.', action: 'Share report', next: '/share-report' },
    'share-report': { title: 'Share report', subtitle: 'North corridor · safety report', icon: 'share', body: 'Choose a secure handoff. The report will retain its evidence hash and engineer sign-off.', action: 'Prepare share link', next: '/reports' },
    'track-health': { title: 'Track health', subtitle: 'Condition score by corridor', icon: 'trending-up', body: 'Network health is 94.2%. North Loop is 88.4% because of the open CRK-2048 restriction.', action: 'Open health history', next: '/health-history' },
    'health-history': { title: 'Health history', subtitle: 'Network score · Apr—Jun 2024', icon: 'bar-chart-2', body: 'Health recovered 1.8 points after the South Yard fastening campaign. North Loop remains the watch corridor.', action: 'Open crack analytics', next: '/crack-analytics' },
    'crack-analytics': { title: 'Crack analytics', subtitle: 'Growth, density, and severity', icon: 'activity', body: '7 new crack-like findings this month. Median confidence is 84%; two records exceed the growth threshold.', action: 'Open risk analytics', next: '/risk-analytics' },
    'risk-analytics': { title: 'Risk analytics', subtitle: 'Intervention thresholds', icon: 'alert-triangle', body: 'North Loop contributes 42% of current network risk. The active restriction is correctly reflected in the score.', action: 'Open maintenance analytics', next: '/maintenance-analytics' },
    'maintenance-analytics': { title: 'Maintenance analytics', subtitle: 'Response time and completion', icon: 'bar-chart-2', body: 'Median response time is 18.4 hours. 91% of tasks close inside their planned window.', action: 'View maintenance', next: '/maintenance' },
    'inspection-calendar': { title: 'Inspection calendar', subtitle: 'June 2024 · coverage plan', icon: 'calendar', body: 'Three inspections are planned this week. West Cut needs an assignee before Friday.', action: 'Create inspection', next: '/create-inspection' },
    'create-inspection': { title: 'Create inspection', subtitle: 'Schedule a corridor run', icon: 'calendar', body: 'Choose a section, capture profile, and engineer. New runs remain editable until they start.', action: 'Save planned inspection', next: '/assigned-inspections' },
    'assigned-inspections': { title: 'Assigned inspections', subtitle: 'Your field queue', icon: 'user', body: 'North Loop is active now. South Yard is next tomorrow at 13:30. One unassigned run needs coverage.', action: 'Open active inspection', next: '/live-inspection' },
    profile: { title: 'E. Chen', subtitle: 'Rail safety lead · Unit 04', icon: 'user', body: 'North corridor access · Last sync 2 minutes ago. Your verification signature is enabled for critical findings.', action: 'Open account settings', next: '/account' },
  };
  const item = config[kind] ?? { title: 'RailGuard', subtitle: 'Field engineering control', icon: 'info', body: 'This module is connected to the RailGuard evidence trail and local field workspace.', action: 'Return to overview', next: '/' };
  return <><Header title={item.title} subtitle={item.subtitle} icon={item.icon} back /><View style={[styles.featureIcon, { backgroundColor: colors.secondary }]}><Feather name={iconFor(item.icon)} size={26} color={colors.primary} /></View><Text style={[styles.featureBody, { color: colors.foreground }]}>{item.body}</Text><RailCard><View style={[styles.infoRow, { borderBottomColor: colors.border }]}><Text style={[styles.detailLabel, { color: colors.mutedForeground }]}>SOURCE</Text><Text style={[styles.rowTitle, { color: colors.foreground }]}>Local field record</Text></View><View style={[styles.infoRow, { borderBottomColor: colors.border }]}><Text style={[styles.detailLabel, { color: colors.mutedForeground }]}>UPDATED</Text><Text style={[styles.rowTitle, { color: colors.foreground }]}>Just now · Unit 04</Text></View></RailCard>{item.action ? <PrimaryButton title={item.action} icon="arrow-right" onPress={() => router.push((item.next ?? '/') as never)} /> : null}</>;
}

function SettingsDetail({ kind }: { kind: string }) {
  const colors = useColors();
  const [enabled, setEnabled] = useState(kind !== 'notification-settings');
  const [stored, setStored] = useState(false);
  useEffect(() => { AsyncStorage.getItem(`railguard:${kind}`).then((value) => { if (value) setEnabled(value === 'true'); }); }, [kind]);
  const toggle = (value: boolean) => { setEnabled(value); AsyncStorage.setItem(`railguard:${kind}`, String(value)); };
  const titles: Record<string, string> = { account: 'Account', 'app-settings': 'App settings', 'notification-settings': 'Notification settings', security: 'Security', language: 'Language', 'help-center': 'Help center', about: 'About RailGuard' };
  if (kind === 'help-center') return <><Header title="Help center" subtitle="Guides for field teams" back /><ListRow icon="camera" title="Capturing a clean rail frame" subtitle="Position, focus, and lighting" tone="info" onPress={() => setStored(!stored)} /><ListRow icon="alert-triangle" title="Understanding risk scores" subtitle="Confidence versus operational risk" tone="warning" onPress={() => setStored(!stored)} /><ListRow icon="shield" title="Signing an engineer verification" subtitle="Audit trail and restrictions" tone="healthy" onPress={() => setStored(!stored)} />{stored ? <EmptyState icon="check" title="Guide marked for later" body="Your field guide bookmark is stored on this device." /> : null}</>;
  return <><Header title={titles[kind] ?? 'Settings'} subtitle="RailGuard workspace" back /><RailCard style={styles.settingCard}><View style={styles.settingRow}><View style={styles.rowMain}><Text style={[styles.rowTitle, { color: colors.foreground }]}>{kind === 'language' ? 'English (United States)' : kind === 'account' ? 'E. Chen' : kind === 'security' ? 'Require passcode on launch' : kind === 'about' ? 'RailGuard 1.0.0' : 'Field mode'}</Text><Text style={[styles.rowSubtitle, { color: colors.mutedForeground }]}>{kind === 'about' ? 'Built for inspectors and safety leads' : 'Saved locally for this device'}</Text></View>{kind === 'language' || kind === 'about' || kind === 'account' ? <Feather name="chevron-right" size={17} color={colors.mutedForeground} /> : <Switch value={enabled} onValueChange={toggle} trackColor={{ false: colors.secondary, true: colors.primary }} thumbColor={colors.foreground} />}</View></RailCard><SectionLabel>LOCAL PERSISTENCE</SectionLabel><Text style={[styles.featureBody, { color: colors.mutedForeground }]}>Settings are available offline and stored securely on this device. {stored ? 'Saved.' : ''}</Text><PrimaryButton title="Save setting" icon="check" onPress={() => { setStored(true); AsyncStorage.setItem(`railguard:${kind}`, String(enabled)); }} /></>;
}

function NotificationsScreen() { return <><Header title="Notifications" subtitle="Signals from your network" back /><ListRow icon="alert-triangle" title="CRK-2048 requires verification" subtitle="North Loop · 12 min ago" tone="critical" onPress={() => router.push('/crack-details')} /><ListRow icon="tool" title="MT-881 due in 4 hours" subtitle="North Loop · 18 min ago" tone="warning" onPress={() => router.push('/task-details')} /><ListRow icon="check-circle" title="Inspection synced successfully" subtitle="East Junction · Yesterday" tone="healthy" onPress={() => router.push('/inspection-summary')} /></>; }

const screenLabels: Record<string, string> = {
  'screen-directory': 'Screen directory',
  'inspection-setup': 'Inspection setup',
  'live-inspection': 'Live inspection',
  'detection-result': 'Detection result',
  'crack-details': 'Crack details',
  'crack-measurement': 'Crack measurement',
  'object-detection': 'Object detection',
  'alignment-analysis': 'Alignment analysis',
  'vibration-analysis': 'Vibration analysis',
  gps: 'GPS',
  'inspection-summary': 'Inspection summary',
  'save-inspection': 'Save inspection',
  defects: 'Defects',
  'defect-list': 'Defect list',
  'defect-details': 'Defect details',
  'crack-history': 'Crack history',
  'image-comparison': 'Image comparison',
  'growth-analysis': 'Growth analysis',
  evidence: 'Evidence',
  comments: 'Comments',
  'engineer-verification': 'Engineer verification',
  map: 'Map',
  'railway-map': 'Railway map',
  'defect-map': 'Defect map',
  'track-map': 'Track map',
  'location-details': 'Location details',
  'risk-heatmap': 'Risk heatmap',
  maintenance: 'Maintenance',
  'maintenance-dashboard': 'Maintenance dashboard',
  'maintenance-task': 'Maintenance task',
  'task-details': 'Task details',
  'before-after': 'Before / after',
  'maintenance-verification': 'Maintenance verification',
  reports: 'Reports',
  'report-details': 'Report details',
  'evidence-package': 'Evidence package',
  'pdf-preview': 'PDF preview',
  'share-report': 'Share report',
  analytics: 'Analytics',
  'track-health': 'Track health',
  'health-history': 'Health history',
  'crack-analytics': 'Crack analytics',
  'risk-analytics': 'Risk analytics',
  'maintenance-analytics': 'Maintenance analytics',
  scheduling: 'Scheduling',
  'inspection-schedule': 'Inspection schedule',
  'inspection-calendar': 'Inspection calendar',
  'create-inspection': 'Create inspection',
  'assigned-inspections': 'Assigned inspections',
  settings: 'Settings',
  account: 'Account',
  'app-settings': 'App settings',
  'notification-settings': 'Notification settings',
  security: 'Security',
  language: 'Language',
  'help-center': 'Help center',
  about: 'About RailGuard',
};

function ScreenDirectory() {
  const colors = useColors();
  const groups: Array<{ title: string; keys: string[] }> = [
    { title: 'AUTHENTICATION', keys: ['splash', 'onboarding', 'login', 'registration', 'otp', 'email-verification', 'forgot-password', 'reset-password'] },
    { title: 'MAIN', keys: ['home', 'notifications', 'profile'] },
    { title: 'INSPECTION', keys: ['inspection-setup', 'live-inspection', 'camera', 'detection-result', 'crack-details', 'crack-measurement', 'object-detection', 'alignment-analysis', 'vibration-analysis', 'gps', 'inspection-summary', 'save-inspection'] },
    { title: 'DEFECTS & EVIDENCE', keys: ['defects', 'defect-list', 'defect-details', 'crack-history', 'image-comparison', 'growth-analysis', 'evidence', 'comments', 'engineer-verification'] },
    { title: 'MAP', keys: ['map', 'railway-map', 'defect-map', 'track-map', 'location-details', 'risk-heatmap'] },
    { title: 'MAINTENANCE', keys: ['maintenance', 'maintenance-dashboard', 'maintenance-task', 'task-details', 'before-after', 'maintenance-verification'] },
    { title: 'REPORTS & ANALYTICS', keys: ['reports', 'report-details', 'evidence-package', 'pdf-preview', 'share-report', 'analytics', 'track-health', 'health-history', 'crack-analytics', 'risk-analytics', 'maintenance-analytics'] },
    { title: 'SCHEDULING & SETTINGS', keys: ['scheduling', 'inspection-schedule', 'create-inspection', 'assigned-inspections', 'inspection-calendar', 'settings', 'account', 'app-settings', 'notification-settings', 'security', 'language', 'help-center', 'about'] },
  ];
  return (
    <>
      <Header title="Screen directory" subtitle={`${SCREEN_REGISTRY.length - 1} linked RailGuard surfaces`} back />
      <Text style={[styles.directoryIntro, { color: colors.mutedForeground }]}>Every workflow is available from this index. Use it to review each state and confirm the handoff to the next step.</Text>
      {groups.map((group) => (
        <View key={group.title}>
          <SectionLabel>{group.title}</SectionLabel>
          {group.keys.map((key) => (
            <ListRow
              key={key}
              icon={iconFor(key)}
              title={screenLabels[key] ?? key}
              subtitle={`Open ${screenLabels[key] ?? key} screen`}
              tone={key.includes('crack') || key.includes('defect') || key === 'risk-heatmap' ? 'critical' : key.includes('maintenance') || key.includes('task') ? 'warning' : 'info'}
              onPress={() => router.push(`/${key}`)}
            />
          ))}
        </View>
      ))}
    </>
  );
}

export default function RailGuardScreen({ screen = '/' }: { screen?: ScreenKey }) {
  const insets = useSafeAreaInsets();
  const key = screen.replace(/^\//, '') || '/';
  const content = key === '/' || key === 'home' ? <HomeScreen /> : key === 'inspections' ? <InspectionsScreen /> : key === 'defects' ? <DefectsScreen /> : key === 'defect-list' ? <DefectsScreen variant="defect-list" /> : key === 'map' ? <MapScreen /> : key === 'railway-map' ? <MapScreen variant="railway-map" /> : key === 'maintenance' ? <MaintenanceScreen /> : key === 'maintenance-dashboard' ? <MaintenanceScreen variant="maintenance-dashboard" /> : key === 'reports' ? <ReportsScreen /> : key === 'analytics' ? <AnalyticsScreen /> : key === 'scheduling' ? <SchedulingScreen /> : key === 'inspection-schedule' ? <SchedulingScreen variant="inspection-schedule" /> : key === 'settings' ? <SettingsScreen /> : key === 'notifications' ? <NotificationsScreen /> : key === 'screen-directory' ? <ScreenDirectory /> : ['splash', 'login', 'registration', 'forgot-password', 'reset-password', 'otp', 'email-verification', 'onboarding'].includes(key) ? <FormScreen kind={key} /> : ['account', 'app-settings', 'notification-settings', 'security', 'language', 'help-center', 'about'].includes(key) ? <SettingsDetail kind={key} /> : key === 'inspection-setup' ? <InspectionSetup /> : key === 'live-inspection' ? <LiveInspection /> : key === 'detection-result' ? <DetectionResult /> : <DetailScreen kind={key} />;
  return <View style={[styles.root, { backgroundColor: useColors().background, paddingTop: Platform.OS === 'web' ? 67 : insets.top }]}><ScrollView contentContainerStyle={[styles.content, { paddingBottom: Math.max(insets.bottom, Platform.OS === 'web' ? 34 : 24) }]} showsVerticalScrollIndicator={false}>{content}</ScrollView></View>;
}

const styles = StyleSheet.create({
  root: { flex: 1 },
  content: { paddingHorizontal: 18, paddingTop: 14, gap: 12, maxWidth: 620, width: '100%', alignSelf: 'center' },
  header: { minHeight: 60, flexDirection: 'row', alignItems: 'center', gap: 11, marginBottom: 7 },
  headerCopy: { flex: 1 },
  headerTitle: { fontFamily: 'Inter_700Bold', fontSize: 22, letterSpacing: -0.4 },
  headerSubtitle: { fontFamily: 'Inter_400Regular', fontSize: 12, marginTop: 2 },
  eyebrow: { fontFamily: 'Inter_700Bold', fontSize: 9, letterSpacing: 1.3, marginBottom: 2 },
  logoMark: { borderWidth: 1, alignItems: 'center', justifyContent: 'center' },
  logoImage: { width: '72%', height: '72%', resizeMode: 'contain' },
  iconButton: { width: 38, height: 38, borderWidth: 1, alignItems: 'center', justifyContent: 'center' },
  headerSpacer: { width: 38 },
  sectionLabelRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 11, marginBottom: 1 },
  sectionLabel: { fontFamily: 'Inter_700Bold', fontSize: 10, letterSpacing: 1.5 },
  sectionAction: { fontFamily: 'Inter_600SemiBold', fontSize: 12 },
  card: { borderWidth: 1, padding: 14, marginBottom: 1 },
  cardTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 15, lineHeight: 20 },
  cardBody: { fontFamily: 'Inter_400Regular', fontSize: 13, lineHeight: 20, marginTop: 5 },
  shiftCard: { padding: 15 },
  shiftTop: { flexDirection: 'row', alignItems: 'flex-start', justifyContent: 'space-between', gap: 8 },
  shiftTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 15 },
  shiftMeta: { fontFamily: 'Inter_400Regular', fontSize: 12, marginTop: 4 },
  progressTrack: { height: 5, marginTop: 19, overflow: 'hidden' },
  progressFill: { height: '100%' },
  shiftBottom: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 7 },
  shiftPercent: { fontFamily: 'Inter_700Bold', fontSize: 12 },
  statusPill: { borderWidth: 1, minHeight: 24, paddingHorizontal: 8, flexDirection: 'row', alignItems: 'center', gap: 5, alignSelf: 'flex-start' },
  statusDot: { width: 6, height: 6, borderRadius: 3 },
  statusText: { fontFamily: 'Inter_600SemiBold', fontSize: 10, letterSpacing: 0.4 },
  metricGrid: { flexDirection: 'row', gap: 8 },
  metricTile: { flex: 1, borderWidth: 1, paddingVertical: 13, paddingHorizontal: 10, minHeight: 74 },
  metricValue: { fontFamily: 'Inter_700Bold', fontSize: 22, letterSpacing: -0.5 },
  metricLabel: { fontFamily: 'Inter_400Regular', fontSize: 10, marginTop: 5, lineHeight: 13 },
  activeInspection: { flexDirection: 'row', alignItems: 'center', gap: 11 },
  activeIcon: { width: 40, height: 40, alignItems: 'center', justifyContent: 'center' },
  rowMain: { flex: 1 },
  rowTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 13, lineHeight: 18 },
  rowSubtitle: { fontFamily: 'Inter_400Regular', fontSize: 11, lineHeight: 16, marginTop: 2 },
  listRow: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 12, paddingHorizontal: 11 },
  rowIcon: { width: 32, height: 32, alignItems: 'center', justifyContent: 'center' },
  rowTrailing: { fontFamily: 'Inter_700Bold', fontSize: 11, letterSpacing: 0.4 },
  workspaceGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 7 },
  workspaceItem: { width: '24%', minWidth: 70, flexGrow: 1, minHeight: 62, borderWidth: 1, padding: 9, justifyContent: 'space-between' },
  workspaceLabel: { fontFamily: 'Inter_500Medium', fontSize: 11 },
  primaryButton: { minHeight: 48, paddingHorizontal: 15, flexDirection: 'row', alignItems: 'center', justifyContent: 'center', gap: 9, borderWidth: 1, marginVertical: 3 },
  primaryButtonText: { fontFamily: 'Inter_700Bold', fontSize: 13, letterSpacing: 0.1 },
  emptyState: { borderWidth: 1, padding: 18, alignItems: 'center', marginTop: 4 },
  emptyIcon: { width: 44, height: 44, alignItems: 'center', justifyContent: 'center', marginBottom: 9 },
  emptyTitle: { fontFamily: 'Inter_600SemiBold', fontSize: 14 },
  emptyBody: { fontFamily: 'Inter_400Regular', fontSize: 12, lineHeight: 18, textAlign: 'center', marginTop: 5, marginBottom: 10 },
  filterRow: { flexDirection: 'row', gap: 7, flexWrap: 'wrap' },
  mapCard: { padding: 10 },
  mapGrid: { height: 230, position: 'relative', overflow: 'hidden', justifyContent: 'center', alignItems: 'center' },
  mapLine: { width: '125%', height: 2, marginVertical: 15, opacity: 0.8 },
  mapPin: { position: 'absolute', width: 24, height: 24, alignItems: 'center', justifyContent: 'center', borderRadius: 12 },
  mapLegend: { flexDirection: 'row', gap: 6, marginTop: 10 },
  reportQuote: { marginTop: 5, gap: 8 },
  healthCard: { padding: 15 },
  healthTop: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'flex-start' },
  healthValue: { fontFamily: 'Inter_700Bold', fontSize: 35, letterSpacing: -1 },
  chart: { height: 120, flexDirection: 'row', alignItems: 'flex-end', gap: 7, marginTop: 16, borderBottomWidth: 1 },
  barWrap: { flex: 1, height: '100%', justifyContent: 'flex-end' },
  bar: { width: '100%', minHeight: 4 },
  chartLabels: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 7 },
  buttonRow: { flexDirection: 'row', gap: 8 },
  scanFrame: { height: 300, borderWidth: 1, alignItems: 'center', justifyContent: 'center', position: 'relative' },
  scanCorner: { position: 'absolute', width: 25, height: 25, borderWidth: 2 },
  cornerTL: { top: 18, left: 18, borderRightWidth: 0, borderBottomWidth: 0 },
  cornerTR: { top: 18, right: 18, borderLeftWidth: 0, borderBottomWidth: 0 },
  cornerBL: { bottom: 18, left: 18, borderRightWidth: 0, borderTopWidth: 0 },
  cornerBR: { bottom: 18, right: 18, borderLeftWidth: 0, borderTopWidth: 0 },
  scanRail: { flexDirection: 'row', gap: 20, transform: [{ rotate: '-19deg' }] },
  railLine: { width: 5, height: 240 },
  scanBadge: { position: 'absolute', left: 14, top: 13, flexDirection: 'row', gap: 6, alignItems: 'center' },
  liveStats: { flexDirection: 'row', gap: 7 },
  detectionHero: { padding: 10 },
  detectionImage: { height: 190, alignItems: 'center', justifyContent: 'center', position: 'relative' },
  detectionMark: { position: 'absolute', width: 130, height: 48, borderWidth: 2, transform: [{ rotate: '-13deg' }] },
  detectionOverlay: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', paddingTop: 10 },
  detectionConfidence: { fontFamily: 'Inter_700Bold', fontSize: 12 },
  detailGrid: { flexDirection: 'row', justifyContent: 'space-between', marginTop: 16, paddingTop: 12, borderTopWidth: 1 },
  detailLabel: { fontFamily: 'Inter_700Bold', fontSize: 9, letterSpacing: 1 },
  detailValue: { fontFamily: 'Inter_600SemiBold', fontSize: 14, marginTop: 5 },
  detailHeader: { alignItems: 'flex-start', paddingVertical: 8 },
  detailScore: { fontFamily: 'Inter_700Bold', fontSize: 56, letterSpacing: -2, marginTop: 12 },
  featureIcon: { width: 60, height: 60, alignItems: 'center', justifyContent: 'center', marginTop: 8 },
  featureBody: { fontFamily: 'Inter_400Regular', fontSize: 16, lineHeight: 25, marginVertical: 9 },
  infoRow: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', minHeight: 35, borderBottomWidth: 1 },
  authWrap: { flex: 1, paddingTop: 28, gap: 14 },
  authBrand: { alignItems: 'center', marginBottom: 25 },
  brandName: { fontFamily: 'Inter_700Bold', fontSize: 20, letterSpacing: 2.8, marginTop: 10 },
  brandTag: { fontFamily: 'Inter_600SemiBold', fontSize: 9, letterSpacing: 1.7, marginTop: 4 },
  authTitle: { fontFamily: 'Inter_700Bold', fontSize: 28, letterSpacing: -0.7 },
  authBody: { fontFamily: 'Inter_400Regular', fontSize: 14, lineHeight: 21, marginBottom: 7 },
  input: { minHeight: 50, borderWidth: 1, paddingHorizontal: 14, fontFamily: 'Inter_400Regular', fontSize: 14 },
  authLink: { fontFamily: 'Inter_600SemiBold', textAlign: 'center', fontSize: 13, marginVertical: 3 },
  directoryIntro: { fontFamily: 'Inter_400Regular', fontSize: 13, lineHeight: 20, marginTop: 2 },
  onboardingRow: { flexDirection: 'row', gap: 11, alignItems: 'center', paddingVertical: 9 },
  onboardingIcon: { width: 42, height: 42, justifyContent: 'center', alignItems: 'center' },
  inlineMeta: { flexDirection: 'row', alignItems: 'center', gap: 8, flexWrap: 'wrap', marginTop: 13 },
  settingCard: { paddingVertical: 4 },
  settingRow: { minHeight: 62, flexDirection: 'row', alignItems: 'center' },
});