/**
 * Semantic design tokens for the mobile app.
 *
 * These tokens mirror the naming conventions used in web artifacts (index.css)
 * so that multi-artifact projects share a cohesive visual identity.
 *
 * Replace the placeholder values below with values that match the project's
 * brand. If a sibling web artifact exists, read its index.css and convert the
 * HSL values to hex so both artifacts use the same palette.
 *
 * To add dark mode, add a `dark` key with the same token names.
 * The useColors() hook will automatically pick it up.
 */

const colors = {
  light: {
    text: '#F3F5F7',
    tint: '#6FA8FF',
    background: '#0B0E11',
    foreground: '#F3F5F7',
    card: '#151A1F',
    cardForeground: '#F3F5F7',
    primary: '#6FA8FF',
    primaryForeground: '#08101E',
    secondary: '#20272E',
    secondaryForeground: '#D8E0E7',
    muted: '#20272E',
    mutedForeground: '#8D99A5',
    accent: '#26313A',
    accentForeground: '#E9EFF4',
    destructive: '#E95D5D',
    destructiveForeground: '#FFF7F7',
    border: '#29323A',
    input: '#1B2228',
    healthy: '#61C58A',
    warning: '#E2B45A',
    info: '#6FA8FF',
    critical: '#E95D5D',
    surfaceRaised: '#1B2228',
    surfaceInset: '#101419',
  },
  dark: {
    text: '#F3F5F7',
    tint: '#6FA8FF',
    background: '#0B0E11',
    foreground: '#F3F5F7',
    card: '#151A1F',
    cardForeground: '#F3F5F7',
    primary: '#6FA8FF',
    primaryForeground: '#08101E',
    secondary: '#20272E',
    secondaryForeground: '#D8E0E7',
    muted: '#20272E',
    mutedForeground: '#8D99A5',
    accent: '#26313A',
    accentForeground: '#E9EFF4',
    destructive: '#E95D5D',
    destructiveForeground: '#FFF7F7',
    border: '#29323A',
    input: '#1B2228',
    healthy: '#61C58A',
    warning: '#E2B45A',
    info: '#6FA8FF',
    critical: '#E95D5D',
    surfaceRaised: '#1B2228',
    surfaceInset: '#101419',
  },

  // Border radius (in px). Sync from the sibling web artifact's --radius
  // CSS variable. This value applies to cards, buttons, inputs, and modals.
  radius: 8,
};

export default colors;
