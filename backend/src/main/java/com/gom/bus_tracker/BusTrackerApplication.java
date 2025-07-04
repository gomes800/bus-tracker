package com.gom.bus_tracker;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class BusTrackerApplication {

	public static void main(String[] args) {
		SpringApplication.run(BusTrackerApplication.class, args);
	}

}
