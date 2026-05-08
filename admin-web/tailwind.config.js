/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          DEFAULT: '#2E7D32',
          dark: '#1B5E20',
          light: '#4CAF50',
        },
        secondary: {
          DEFAULT: '#1565C0',
          dark: '#0D47A1',
          light: '#42A5F5',
        },
        accent: '#FF6F00',
      },
    },
  },
  plugins: [],
}
