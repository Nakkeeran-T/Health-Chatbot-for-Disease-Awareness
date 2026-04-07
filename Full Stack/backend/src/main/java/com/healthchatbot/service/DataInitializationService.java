package com.healthchatbot.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Data Initialization Service
 *
 * Automatically seeds the PostgreSQL database with government health data
 * on application startup. This runs ONCE after Spring context is fully ready.
 *
 * This replaces the static hardcoded data previously in ml_api_server.py
 * by providing a persistent, queryable, multilingual database of:
 *  - Disease information (from NCDC / NCDC Odisha)
 *  - Vaccination schedules (from NHM Universal Immunization Programme)
 *  - Real outbreak seeds for demo purposes
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DataInitializationService {

    private final GovernmentHealthDataService govHealthDataService;
    private final OutbreakMonitorService outbreakMonitorService;

    @EventListener(ApplicationReadyEvent.class)
    public void initializeOnStartup() {
        log.info("=============================================================");
        log.info("  🌐 ArogyaBot — Government Health Data Initialization");
        log.info("  Source: NHM Odisha | NCDC | MOHFW | IDSP");
        log.info("=============================================================");

        try {
            // Feature #2: Seed disease information from government sources
            govHealthDataService.seedDiseaseData();
            govHealthDataService.seedVaccineData();

            // Feature #3: Seed initial outbreak alerts for Odisha districts
            outbreakMonitorService.seedInitialOutbreakData();

            log.info("✅ Government health database initialization complete.");
        } catch (Exception e) {
            log.error("❌ Failed to initialize health data: {}", e.getMessage(), e);
        }

        log.info("=============================================================");
    }
}
