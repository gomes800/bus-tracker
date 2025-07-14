# Bus Tracker Frontend

This is an Angular 19 application that displays bus positions on an interactive map using OpenStreetMap.

## Features

- **Interactive Map**: Uses OpenStreetMap with Leaflet for map display
- **User Location**: Get your current location and center the map on it
- **Bus Tracking**: Display real-time bus positions for any bus line
- **Auto Refresh**: Automatically refresh bus positions every 30 seconds
- **Responsive Design**: Works on desktop and mobile devices

## Prerequisites

- Node.js (version 18 or higher)
- Angular CLI
- Backend API running (default: http://localhost:8080)

## Installation

1. Install dependencies:

```bash
npm install
```

2. Start the development server:

```bash
npm start
```

3. Open your browser and navigate to `http://localhost:4200`

## Usage

1. **Get Your Location**: Click the "Get My Location" button to center the map on your current position
2. **Enter Bus Line**: Type a bus line number in the input field
3. **Load Buses**: Click "Load Buses" to fetch and display bus positions for that line
4. **Auto Refresh**: Click "Start Auto Refresh" to automatically update bus positions every 30 seconds

## API Configuration

The application expects the backend API to be running on `http://localhost:8080`. You can modify the API URL in `src/app/services/bus.service.ts` if needed.

## Backend API Endpoints

- `GET /bus/positions?line={lineNumber}` - Returns bus positions for a specific line

## Technologies Used

- Angular 19
- Leaflet (OpenStreetMap integration)
- TypeScript
- SCSS for styling

## Development

- Run `ng serve` for a dev server
- Run `ng build` to build the project
- Run `ng test` to execute unit tests

## Browser Compatibility

This application requires a modern browser with geolocation support. Make sure to allow location access when prompted.
