package softuni.exam.service.impl;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dto.ImportTownDTO;
import softuni.exam.models.entity.Town;
import softuni.exam.repository.TownRepository;
import softuni.exam.service.TownService;
import softuni.exam.util.ValidationUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TownServiceImpl implements TownService {
    private static final String TOWNS_FILE_PATH = "src/main/resources/files/json/towns.json";
    private final TownRepository townRepository;
    private final Gson gson;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;

    public TownServiceImpl(TownRepository townRepository, Gson gson, ValidationUtil validationUtil, ModelMapper modelMapper) {
        this.townRepository = townRepository;
        this.gson = gson;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
    }

    @Override
    public boolean areImported() {
        return townRepository.count() > 0;
    }

    @Override
    public String readTownsFileContent() throws IOException {
        return Files.readString(Path.of(TOWNS_FILE_PATH));
    }

    @Override
    public String importTowns() throws IOException {
        ImportTownDTO[] importTownDTOS = gson
                .fromJson(readTownsFileContent(), ImportTownDTO[].class);

        List<String> result = new ArrayList<>();

        for (ImportTownDTO importTownDTO : importTownDTOS) {
            if (validationUtil.isValid(importTownDTO)) {
                Town town = modelMapper.map(importTownDTO, Town.class);

                townRepository.save(town);

                String msg = String.format("Successfully imported town %s - %d",
                        town.getTownName(),
                        town.getPopulation());

                result.add(msg);
            } else {
                result.add("Invalid town");
            }
        }
        return String.join("\n", result);
    }
}
