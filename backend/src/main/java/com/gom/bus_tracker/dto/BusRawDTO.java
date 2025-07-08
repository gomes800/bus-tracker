package com.gom.bus_tracker.dto;

import lombok.Data;

@Data
public class BusRawDTO {
    private String ordem;
    private String linha;
    private String latitude;
    private String longitude;
    private String datahoraservidor;
    private String datahora;
    private String velocidade;
    private String datahoraenvio;
}
