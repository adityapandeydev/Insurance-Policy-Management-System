package com.insurance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * ╔══════════════════════════════════════════════════════════════════════════╗
 * ║           INSURANCE POLICY MANAGEMENT SYSTEM — ENTRY POINT              ║
 * ╠══════════════════════════════════════════════════════════════════════════╣
 * ║                                                                          ║
 * ║  This is the main Spring Boot application class — the entry point for   ║
 * ║  the entire application. When this class is run, Spring Boot:           ║
 * ║  1. Creates the ApplicationContext (IoC container)                      ║
 * ║  2. Triggers component scanning from this package downward              ║
 * ║  3. Auto-configures beans based on classpath dependencies               ║
 * ║  4. Starts the embedded Tomcat server                                   ║
 * ║  5. Applies Flyway database migrations                                  ║
 * ║                                                                          ║
 * ║  INTERVIEW TIP: @SpringBootApplication is a meta-annotation that        ║
 * ║  combines three annotations:                                             ║
 * ║  ① @Configuration     → This class can define @Bean methods             ║
 * ║  ② @EnableAutoConfiguration → Enables Spring Boot's auto-configuration  ║
 * ║     (scans META-INF/spring.factories and conditionally creates beans)   ║
 * ║  ③ @ComponentScan     → Scans all sub-packages for @Component,          ║
 * ║     @Service, @Repository, @Controller annotated classes                ║
 * ║                                                                          ║
 * ╚══════════════════════════════════════════════════════════════════════════╝
 *
 * @author Insurance System Team
 * @version 1.0.0
 * @since Java 21, Spring Boot 3.2.3
 */
@SpringBootApplication

/**
 * @EnableJpaAuditing
 * ─────────────────────────────────────────────────────────────────────────
 * Enables JPA Auditing, which automatically populates audit fields:
 * @CreatedDate  → sets the timestamp when entity is first persisted
 * @LastModifiedDate → updates timestamp on every update
 * @CreatedBy    → sets the user who created the entity (needs AuditorAware)
 *
 * INTERVIEW TIP: Without this annotation, @CreatedDate and @LastModifiedDate
 * annotations on entity fields won't work — they'll remain null.
 * The AuditingEntityListener also needs to be declared on each entity
 * with @EntityListeners(AuditingEntityListener.class)
 */
@EnableJpaAuditing

/**
 * @EnableScheduling
 * ─────────────────────────────────────────────────────────────────────────
 * Enables Spring's task scheduling infrastructure.
 * Allows @Scheduled methods to run on a cron expression or fixed rate.
 * Used in this system for: policy expiry checks, scheduled risk reassessment.
 */
@EnableScheduling
public class InsurancePolicyManagementApplication {

    /**
     * Application entry point.
     *
     * SpringApplication.run() does the following:
     * 1. Creates a ConfigurableApplicationContext
     * 2. Registers a shutdown hook for graceful shutdown
     * 3. Triggers ApplicationStartedEvent, ApplicationReadyEvent
     * 4. Returns the fully initialized ApplicationContext
     *
     * @param args Command-line arguments (can pass --server.port=9090 etc.)
     */
    public static void main(String[] args) {
        SpringApplication.run(InsurancePolicyManagementApplication.class, args);
    }
}
