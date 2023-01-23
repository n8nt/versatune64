package com.datvexpress.ws.versatune.repo;

import com.datvexpress.ws.versatune.model.TunerConfigRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TunerSetupRepository extends JpaRepository<TunerConfigRecord, Long> {

}
