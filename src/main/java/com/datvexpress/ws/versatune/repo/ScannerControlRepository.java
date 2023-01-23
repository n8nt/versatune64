package com.datvexpress.ws.versatune.repo;

import com.datvexpress.ws.versatune.model.ScannerControlRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ScannerControlRepository extends JpaRepository<ScannerControlRecord, Long> {
}
