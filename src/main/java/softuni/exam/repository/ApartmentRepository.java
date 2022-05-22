package softuni.exam.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import softuni.exam.models.entity.Apartment;

import java.util.Optional;

@Repository
public interface ApartmentRepository extends JpaRepository<Apartment, Long> {
    @Query("SELECT a FROM Apartment a WHERE a.town.townName = :town AND a.area = :area")
    Optional<Apartment> findByTownNameAndArea(@Param(value = "town") String town,
                                              @Param(value = "area") Double area);
}
