package com.healthsync.repository;

import com.healthsync.model.Ward;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WardRepository extends JpaRepository<Ward, String> {
    List<Ward> findByWardType(Ward.WardType type);
    List<Ward> findByAvailableBedsGreaterThan(int count);
}
