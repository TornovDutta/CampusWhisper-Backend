package com.campus.whisper.repository;

import com.campus.whisper.models.SafeReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SafeReportRepository extends JpaRepository<SafeReport, Long> {
}
