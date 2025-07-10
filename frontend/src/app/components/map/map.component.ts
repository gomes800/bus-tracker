import {
  Component,
  OnInit,
  OnDestroy,
  ElementRef,
  ViewChild,
  Input,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { BusService, BusPosition } from '../../services/bus.service';
import { LocationService, UserLocation } from '../../services/location.service';
import { interval, Subscription } from 'rxjs';
import { switchMap, catchError } from 'rxjs/operators';
import * as L from 'leaflet';

@Component({
  selector: 'app-map',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="map-container">
      <div class="controls">
        <div class="input-group">
          <input
            #lineInput
            type="text"
            placeholder="Digite o número da linha"
            [(ngModel)]="selectedLine"
            (keyup.enter)="loadBusPositions()"
            class="line-input"
          />
          <button (click)="loadBusPositions()" class="load-btn">
            <span class="btn-text">Carregar</span>
            <span class="btn-icon">🚌</span>
          </button>
        </div>

        <div class="button-group">
          <button (click)="getUserLocation()" class="location-btn">
            <span class="btn-text">Minha Localização</span>
            <span class="btn-icon">📍</span>
          </button>
          <button (click)="toggleAutoRefresh()" class="refresh-btn">
            <span class="btn-text">{{
              autoRefresh ? 'Parar Auto' : 'Auto Atualizar'
            }}</span>
            <span class="btn-icon">{{ autoRefresh ? '⏹️' : '🔄' }}</span>
          </button>
        </div>
      </div>

      <div #mapContainer class="map" id="map"></div>

      <div class="status" *ngIf="statusMessage">
        <span class="status-icon">ℹ️</span>
        {{ statusMessage }}
      </div>
    </div>
  `,
  styles: [
    `
      .map-container {
        display: flex;
        flex-direction: column;
        height: 100vh;
        width: 100%;
      }

      .controls {
        display: flex;
        flex-direction: column;
        gap: 12px;
        padding: 16px;
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        box-shadow: 0 2px 10px rgba(0, 0, 0, 0.1);
      }

      .input-group {
        display: flex;
        gap: 8px;
        width: 100%;
      }

      .line-input {
        flex: 1;
        padding: 12px 16px;
        border: none;
        border-radius: 8px;
        font-size: 16px;
        background: rgba(255, 255, 255, 0.9);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }

      .line-input:focus {
        outline: none;
        background: white;
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
      }

      .button-group {
        display: flex;
        gap: 8px;
        flex-wrap: wrap;
      }

      .load-btn,
      .location-btn,
      .refresh-btn {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px 16px;
        border: none;
        border-radius: 8px;
        cursor: pointer;
        font-size: 14px;
        font-weight: 600;
        transition: all 0.3s ease;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }

      .load-btn {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        min-width: 100px;
      }

      .load-btn:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
      }

      .location-btn {
        background: linear-gradient(135deg, #11998e 0%, #38ef7d 100%);
        color: white;
        flex: 1;
      }

      .location-btn:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
      }

      .refresh-btn {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        color: white;
        flex: 1;
      }

      .refresh-btn:hover {
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(0, 0, 0, 0.2);
      }

      .btn-text {
        display: inline;
      }

      .btn-icon {
        font-size: 16px;
      }

      .map {
        flex: 1;
        width: 100%;
      }

      .status {
        display: flex;
        align-items: center;
        gap: 8px;
        padding: 12px 16px;
        background: rgba(255, 255, 255, 0.95);
        border-top: 1px solid rgba(0, 0, 0, 0.1);
        font-size: 14px;
        color: #333;
      }

      .status-icon {
        font-size: 16px;
      }

      /* Mobile optimizations */
      @media (max-width: 768px) {
        .controls {
          padding: 12px;
          gap: 8px;
        }

        .input-group {
          flex-direction: column;
          gap: 8px;
        }

        .line-input {
          font-size: 16px;
          padding: 14px 16px;
        }

        .button-group {
          flex-direction: column;
          gap: 8px;
        }

        .load-btn,
        .location-btn,
        .refresh-btn {
          padding: 14px 16px;
          font-size: 16px;
          justify-content: center;
          min-height: 44px;
        }
      }

      /* Small mobile devices */
      @media (max-width: 480px) {
        .controls {
          padding: 8px;
        }

        .line-input {
          padding: 12px 14px;
        }

        .load-btn,
        .location-btn,
        .refresh-btn {
          padding: 12px 14px;
          font-size: 14px;
        }
      }

      /* Landscape mode */
      @media (max-height: 500px) and (orientation: landscape) {
        .controls {
          flex-direction: row;
          align-items: center;
          gap: 12px;
        }

        .input-group {
          flex-direction: row;
          flex: 1;
        }

        .button-group {
          flex-direction: row;
          flex-wrap: wrap;
        }
      }
    `,
  ],
})
export class MapComponent implements OnInit, OnDestroy {
  @ViewChild('mapContainer', { static: true }) mapContainer!: ElementRef;
  @Input() selectedLine: string = '';

  private map!: L.Map;
  private busMarkers: L.Marker[] = [];
  private userMarker?: L.Marker;
  private autoRefreshSubscription?: Subscription;
  public autoRefresh = false;
  statusMessage = '';

  constructor(
    private busService: BusService,
    private locationService: LocationService
  ) {}

  ngOnInit() {
    this.initializeMap();
  }

  ngOnDestroy() {
    if (this.autoRefreshSubscription) {
      this.autoRefreshSubscription.unsubscribe();
    }
    if (this.map) {
      this.map.remove();
    }
  }

  private initializeMap() {
    const defaultLat = -22.8917;
    const defaultLng = -43.2404;

    this.map = L.map(this.mapContainer.nativeElement).setView(
      [defaultLat, defaultLng],
      13
    );

    L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
      attribution: '© OpenStreetMap contributors',
    }).addTo(this.map);

    this.statusMessage =
      'Mapa carregado. Digite o número da linha para carregar os ônibus.';
  }

  getUserLocation() {
    this.statusMessage = 'Obtendo sua localização...';

    this.locationService.getCurrentLocation().subscribe({
      next: (location: UserLocation) => {
        this.centerMapOnLocation(location.latitude, location.longitude);
        this.addUserMarker(location.latitude, location.longitude);
        this.statusMessage = `Localização encontrada: ${location.latitude.toFixed(
          4
        )}, ${location.longitude.toFixed(4)}`;
      },
      error: (error) => {
        this.statusMessage = `Erro ao obter localização: ${error}`;
        console.error('Location error:', error);
      },
    });
  }

  private centerMapOnLocation(lat: number, lng: number) {
    this.map.setView([lat, lng], 15);
  }

  private addUserMarker(lat: number, lng: number) {
    if (this.userMarker) {
      this.map.removeLayer(this.userMarker);
    }

    const userIcon = L.divIcon({
      className: 'user-marker',
      html: '<div style="background-color: #007bff; width: 20px; height: 20px; border-radius: 50%; border: 3px solid white; box-shadow: 0 0 10px rgba(0,0,0,0.3);"></div>',
      iconSize: [20, 20],
      iconAnchor: [10, 10],
    });

    this.userMarker = L.marker([lat, lng], { icon: userIcon })
      .addTo(this.map)
      .bindPopup('Sua Localização');
  }

  loadBusPositions() {
    console.log('Iniciando loadBusPositions');
    if (!this.selectedLine.trim()) {
      this.statusMessage = 'Por favor, digite o número da linha.';
      return;
    }

    this.statusMessage = `Carregando ônibus da linha ${this.selectedLine}...`;
    console.log('Loading bus positions for line:', this.selectedLine);

    this.busService.getBusPositions(this.selectedLine).subscribe({
      next: (busPositions: BusPosition[]) => {
        console.log('Received bus positions in component:', busPositions);
        this.clearBusMarkers();
        this.addBusMarkers(busPositions);
        this.statusMessage = `Carregados ${busPositions.length} ônibus da linha ${this.selectedLine}`;
      },
      error: (error) => {
        this.statusMessage = `Erro ao carregar ônibus: ${error.message}`;
        console.error('Error loading bus positions:', error);
      },
    });
  }

  private clearBusMarkers() {
    console.log('Limpando marcadores antigos:', this.busMarkers.length);
    this.busMarkers.forEach((marker) => {
      this.map.removeLayer(marker);
    });
    this.busMarkers = [];
  }

  private addBusMarkers(busPositions: BusPosition[]) {
    console.log('Adicionando marcadores:', busPositions);
    console.log('Adding bus markers for', busPositions.length, 'buses');

    busPositions.forEach((bus, index) => {
      console.log(`Bus ${index + 1}:`, bus);
      const lat = parseFloat(bus.latitude);
      const lng = parseFloat(bus.longitude);

      console.log(`Parsed coordinates: lat=${lat}, lng=${lng}`);

      if (!isNaN(lat) && !isNaN(lng)) {
        const busIcon = L.divIcon({
          className: 'bus-marker',
          html: '<div style="background-color: #dc3545; width: 16px; height: 16px; border-radius: 50%; border: 2px solid white; box-shadow: 0 0 5px rgba(0,0,0,0.3);"></div>',
          iconSize: [16, 16],
          iconAnchor: [8, 8],
        });

        const marker = L.marker([lat, lng], { icon: busIcon }).addTo(this.map)
          .bindPopup(`
            <strong>Ônibus ${bus.ordem}</strong><br>
            Linha: ${bus.linha}<br>
            Horário: ${bus.datahoraservidor}
          `);

        this.busMarkers.push(marker);
        console.log(`Added marker for bus ${bus.ordem} at [${lat}, ${lng}]`);
      } else {
        console.warn(
          `Invalid coordinates for bus ${bus.ordem}: lat=${bus.latitude}, lng=${bus.longitude}`
        );
      }
    });

    console.log('Total markers added:', this.busMarkers.length);
  }

  toggleAutoRefresh() {
    if (this.autoRefresh) {
      this.stopAutoRefresh();
    } else {
      this.startAutoRefresh();
    }
  }

  private startAutoRefresh() {
    if (!this.selectedLine.trim()) {
      this.statusMessage =
        'Digite o número da linha antes de iniciar a atualização automática.';
      return;
    }

    this.autoRefresh = true;
    this.autoRefreshSubscription = interval(25000)
      .pipe(switchMap(() => this.busService.getBusPositions(this.selectedLine)))
      .subscribe({
        next: (busPositions: BusPosition[]) => {
          console.log('Auto-refresh recebeu:', busPositions);
          this.clearBusMarkers();
          this.addBusMarkers(busPositions);
          this.statusMessage = `Atualizado: ${busPositions.length} ônibus da linha ${this.selectedLine}`;
        },
        error: (error) => {
          this.statusMessage = `Erro na atualização automática: ${error.message}`;
          console.error('Auto-refresh error:', error);
        },
      });

    this.statusMessage = `Atualização automática iniciada para linha ${this.selectedLine}`;
  }

  private stopAutoRefresh() {
    this.autoRefresh = false;
    if (this.autoRefreshSubscription) {
      this.autoRefreshSubscription.unsubscribe();
    }
    this.statusMessage = 'Atualização automática parada.';
  }
}
