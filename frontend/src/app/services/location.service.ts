import { Injectable } from '@angular/core';
import { Observable, from } from 'rxjs';

export interface UserLocation {
  latitude: number;
  longitude: number;
}

@Injectable({
  providedIn: 'root',
})
export class LocationService {
  constructor() {}

  getCurrentLocation(): Observable<UserLocation> {
    return from(
      new Promise<UserLocation>((resolve, reject) => {
        if (!navigator.geolocation) {
          reject('Geolocation is not supported by this browser.');
          return;
        }

        navigator.geolocation.getCurrentPosition(
          (position) => {
            resolve({
              latitude: position.coords.latitude,
              longitude: position.coords.longitude,
            });
          },
          (error) => {
            reject(`Error getting location: ${error.message}`);
          },
          {
            enableHighAccuracy: true,
            timeout: 10000,
            maximumAge: 300000,
          }
        );
      })
    );
  }
}
