package com.ageoverflow.gcas;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

// REF: The structure of this application was inspired by a YouTube tutorial referenced in
// refs.md
@SpringBootApplication
@EnableAsync
public class GcasApiApplication {

	public static void main(String[] args) {
		SpringApplication.run(GcasApiApplication.class, args);
	}

}
