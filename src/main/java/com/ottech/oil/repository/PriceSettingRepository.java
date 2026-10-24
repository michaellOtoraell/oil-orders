package com.ottech.oil.repository;

import com.ottech.oil.entity.PriceSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PriceSettingRepository extends JpaRepository<PriceSetting, Long> {
    Optional<PriceSetting> findFirstByOrderByIdDesc();
    List<PriceSetting> findTop20ByOrderByIdDesc();
}
