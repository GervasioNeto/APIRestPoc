package com.example.painel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class PainelApplication {

	public static void main(String[] args) {
		SpringApplication.run(PainelApplication.class, args);
	}

}
