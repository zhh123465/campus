---
name: Academic Pulse
colors:
  surface: '#f8f9ff'
  surface-dim: '#cbdbf5'
  surface-bright: '#f8f9ff'
  surface-container-lowest: '#ffffff'
  surface-container-low: '#eff4ff'
  surface-container: '#e5eeff'
  surface-container-high: '#dce9ff'
  surface-container-highest: '#d3e4fe'
  on-surface: '#0b1c30'
  on-surface-variant: '#424754'
  inverse-surface: '#213145'
  inverse-on-surface: '#eaf1ff'
  outline: '#727785'
  outline-variant: '#c2c6d6'
  surface-tint: '#005ac2'
  primary: '#0058be'
  on-primary: '#ffffff'
  primary-container: '#2170e4'
  on-primary-container: '#fefcff'
  inverse-primary: '#adc6ff'
  secondary: '#006c49'
  on-secondary: '#ffffff'
  secondary-container: '#6cf8bb'
  on-secondary-container: '#00714d'
  tertiary: '#924700'
  on-tertiary: '#ffffff'
  tertiary-container: '#b75b00'
  on-tertiary-container: '#fffbff'
  error: '#ba1a1a'
  on-error: '#ffffff'
  error-container: '#ffdad6'
  on-error-container: '#93000a'
  primary-fixed: '#d8e2ff'
  primary-fixed-dim: '#adc6ff'
  on-primary-fixed: '#001a42'
  on-primary-fixed-variant: '#004395'
  secondary-fixed: '#6ffbbe'
  secondary-fixed-dim: '#4edea3'
  on-secondary-fixed: '#002113'
  on-secondary-fixed-variant: '#005236'
  tertiary-fixed: '#ffdcc6'
  tertiary-fixed-dim: '#ffb786'
  on-tertiary-fixed: '#311400'
  on-tertiary-fixed-variant: '#723600'
  background: '#f8f9ff'
  on-background: '#0b1c30'
  surface-variant: '#d3e4fe'
typography:
  display-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 48px
    fontWeight: '800'
    lineHeight: '1.1'
    letterSpacing: -0.02em
  headline-lg:
    fontFamily: Plus Jakarta Sans
    fontSize: 32px
    fontWeight: '700'
    lineHeight: '1.2'
    letterSpacing: -0.01em
  headline-lg-mobile:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '700'
    lineHeight: '1.2'
  headline-md:
    fontFamily: Plus Jakarta Sans
    fontSize: 24px
    fontWeight: '600'
    lineHeight: '1.3'
  body-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '400'
    lineHeight: '1.6'
  body-md:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: '1.5'
  label-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: '1.2'
    letterSpacing: 0.01em
  label-sm:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: '1.2'
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  base: 4px
  xs: 8px
  sm: 16px
  md: 24px
  lg: 48px
  xl: 80px
  container-max: 1200px
  gutter: 24px
  margin-mobile: 16px
---

## Brand & Style
The brand personality is academic, energetic, and professional yet accessible, tailored for a modern campus environment. It balances the rigor of academic life with the vibrant social energy of youth. 

The design style is **Minimalist / Modern**, emphasizing extreme clarity and high whitespace. It draws inspiration from high-end productivity tools, utilizing a "clean and atmospheric" (简约、大气) approach. Visual interest is generated through precise typography and purposeful use of vibrant primary accents rather than decorative ornamentation. The interface should feel "breathable," prioritizing content and community interaction above all else.

## Colors
The palette is rooted in a "Clean White" foundation to maximize the sense of space. 
- **Primary:** Bright Blue (#3B82F6) serves as the main driver for actions and active states, symbolizing intelligence and clarity.
- **Secondary:** Fresh Mint (#10B981) is used sparingly for success states or specialized campus tags (e.g., "Verified" or "Active").
- **Neutral:** A scale of cool greys handles text hierarchy and borders.
- **Surface:** Soft Grey (#F8FAFC) is used to differentiate card backgrounds or sidebars from the main white canvas, creating subtle depth without relying on heavy lines.

## Typography
This design system employs a dual-font strategy to balance character with utility. **Plus Jakarta Sans** is used for headings to provide a friendly, modern, and youthful geometric feel. **Inter** is utilized for body text and UI labels to ensure maximum readability and a professional, systematic tone. 

High contrast is maintained between headings (Bold/ExtraBold) and body text (Regular). Large display type should use tighter letter spacing to maintain a "tight" editorial look.

## Layout & Spacing
The layout follows a **Fixed Grid** philosophy for desktop to maintain the "Notion-like" structured feel, while transitioning to a fluid model for mobile. 

- **Desktop:** 12-column grid with a 1200px max-width. Use generous 48px or 80px vertical section spacing to maintain the "atmospheric" quality.
- **Gaps:** Use a 24px gutter for card grids.
- **Padding:** Internal card padding should be a minimum of 24px to prevent content from feeling cramped.
- **Mobile:** Transition to a single-column layout with 16px side margins.

## Elevation & Depth
Depth is communicated through **Tonal Layers** and **Ambient Shadows**. 

1. **Level 0 (Base):** Pure White (#FFFFFF).
2. **Level 1 (Surface):** Soft Grey (#F8FAFC) used for large background areas or inset sections.
3. **Level 2 (Cards):** White surfaces elevated by very soft, highly diffused shadows (e.g., `0px 4px 20px rgba(0, 0, 0, 0.05)`). 

Avoid all glassmorphism, blurs, and heavy strokes. Borders should be used sparingly, primarily in a very light grey (#E2E8F0) for input fields or structural separators.

## Shapes
The shape language is defined by large, inviting radii. 
- **Cards/Containers:** Use 12px to 16px (`rounded-lg` or `rounded-xl`) to reinforce the youthful and modern aesthetic.
- **Buttons:** Use 8px (`rounded-md`) for a professional "tool-like" feel, or full pill-shape for social tags/chips.
- **Inputs:** Match button roundedness (8px) for consistency in functional forms.

## Components
- **Buttons:** Primary buttons use solid Bright Blue with white text. Secondary buttons use a light grey ghost style or a subtle blue tint. Use `label-md` for button text.
- **Cards:** White background, 16px border-radius, and the ambient shadow defined in Elevation. Use for forum posts, event listings, and user profiles.
- **Input Fields:** Soft grey backgrounds (#F8FAFC) with a subtle 1px border that turns Blue on focus. Labels should be small and uppercase or semi-bold.
- **Chips/Tags:** Pill-shaped with a light primary tint background (e.g., 10% opacity Blue) and high-contrast text.
- **Lists:** High vertical padding (16px-20px) between list items. Use subtle dividers only when content density requires it.
- **Forum Specifics:** Threaded comments should use a vertical line indicator in a soft grey, rather than boxed backgrounds, to keep the "spacious" look.