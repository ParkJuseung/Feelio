package com.test.feelio.repository;

import com.test.feelio.entity.ActivityEffect;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActivityEffectRepository extends JpaRepository<ActivityEffect, Long> {
    @Query("SELECT AVG(ae.averageChangeScore) FROM ActivityEffect ae WHERE ae.activityName = :activityName")
    Optional<Double> findAverageChangeScore(@Param("activityName") String activityName);
}