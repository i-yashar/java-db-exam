package softuni.exam.service.impl;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dto.ImportApartmentDTO;
import softuni.exam.models.dto.ImportApartmentRootDTO;
import softuni.exam.models.entity.Apartment;
import softuni.exam.repository.ApartmentRepository;
import softuni.exam.repository.TownRepository;
import softuni.exam.service.ApartmentService;
import softuni.exam.util.ValidationUtil;
import softuni.exam.util.XmlParser;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ApartmentServiceImpl implements ApartmentService {
    private static final String APARTMENTS_FILE_PATH = "src/main/resources/files/xml/apartments.xml";
    private final ApartmentRepository apartmentRepository;
    private final TownRepository townRepository;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;
    private final XmlParser xmlParser;

    public ApartmentServiceImpl(ApartmentRepository apartmentRepository, TownRepository townRepository, Gson gson, ValidationUtil validationUtil, ModelMapper modelMapper, XmlParser xmlParser) {
        this.apartmentRepository = apartmentRepository;
        this.townRepository = townRepository;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
        this.xmlParser = xmlParser;
    }

    @Override
    public boolean areImported() {
        return apartmentRepository.count() > 0;
    }

    @Override
    public String readApartmentsFromFile() throws IOException {
        return Files.readString(Path.of(APARTMENTS_FILE_PATH));
    }

    @Override
    public String importApartments() throws IOException, JAXBException {
        ImportApartmentRootDTO importApartmentRootDTO = xmlParser
                .fromFile(APARTMENTS_FILE_PATH, ImportApartmentRootDTO.class);

        return importApartmentRootDTO
                .getApartments()
                .stream()
                .map(this::importApartment)
                .collect(Collectors.joining("\n"));
    }

    private String importApartment(ImportApartmentDTO dto) {
        if(!validationUtil.isValid(dto)) {
            return "Invalid apartment";
        }

        Optional<Apartment> optTown = apartmentRepository.findByTownNameAndArea(dto.getTown(), dto.getArea());

        if(optTown.isPresent()) {
            return "Invalid apartment";
        }

        Apartment apartment = modelMapper.map(dto, Apartment.class);

        apartment.setTown(townRepository.findByTownName(dto.getTown()).get());

        apartmentRepository.save(apartment);

        return String.format("Successfully imported apartment %s - %.2f",
                apartment.getApartmentType().name(),
                apartment.getArea());
    }
}
