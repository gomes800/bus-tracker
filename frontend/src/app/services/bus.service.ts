import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap, catchError } from 'rxjs/operators';

export interface BusPosition {
  ordem: string;
  linha: string;
  longitude: string;
  latitude: string;
  datahoraservidor: string;
}

@Injectable({
  providedIn: 'root',
})
export class BusService {
  private apiUrl = 'http://localhost:8080/bus';

  constructor(private http: HttpClient) {}

  getBusPositions(line: string): Observable<BusPosition[]> {
    console.log(`Fetching bus positions for line: ${line}`);
    console.log(`API URL: ${this.apiUrl}/positions?line=${line}`);

    return this.http
      .get<BusPosition[]>(`${this.apiUrl}/positions?line=${line}`)
      .pipe(
        tap((data) => {
          console.log('Bus positions received:', data);
          console.log('Number of buses:', data.length);
        }),
        catchError((error) => {
          console.error('Error fetching bus positions:', error);
          throw error;
        })
      );
  }
}
