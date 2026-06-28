package com.jai.loglens.repository;

import com.jai.loglens.domain.LogEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface LogEventRepository extends JpaRepository<LogEvent, Long>, JpaSpecificationExecutor<LogEvent> {

    @Query("select l.serviceName, count(l) from LogEvent l "
            + "where l.eventTime between :from and :to "
            + "group by l.serviceName order by count(l) desc")
    List<Object[]> countByServiceBetween(@Param("from") Instant from, @Param("to") Instant to);

    long countByServiceNameAndEventTimeAfter(String serviceName, Instant after);

    @Modifying
    @Query("delete from LogEvent l where l.eventTime < :cutoff")
    int deleteOlderThan(@Param("cutoff") Instant cutoff);

    @Modifying
    @Query("delete from LogEvent l where l.serviceName = :service and l.eventTime >= :from")
    int deleteRecentForService(@Param("service") String service, @Param("from") Instant from);

    @Query("select max(l.eventTime) from LogEvent l")
    Instant findNewestEventTime();
}
