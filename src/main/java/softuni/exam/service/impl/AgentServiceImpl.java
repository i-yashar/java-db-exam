package softuni.exam.service.impl;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dto.ImportAgentDTO;
import softuni.exam.models.entity.Agent;
import softuni.exam.models.entity.Town;
import softuni.exam.repository.AgentRepository;
import softuni.exam.repository.TownRepository;
import softuni.exam.service.AgentService;
import softuni.exam.util.ValidationUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class AgentServiceImpl implements AgentService {
    private static final String AGENTS_FILE_PATH = "src/main/resources/files/json/agents.json";
    private final AgentRepository agentRepository;
    private final TownRepository townRepository;
    private final Gson gson;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;

    public AgentServiceImpl(AgentRepository agentRepository, TownRepository townRepository, Gson gson, ValidationUtil validationUtil, ModelMapper modelMapper) {
        this.agentRepository = agentRepository;
        this.townRepository = townRepository;
        this.gson = gson;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
    }

    @Override
    public boolean areImported() {
        return agentRepository.count() > 0;
    }

    @Override
    public String readAgentsFromFile() throws IOException {
        return Files.readString(Path.of(AGENTS_FILE_PATH));
    }

    @Override
    public String importAgents() throws IOException {
        ImportAgentDTO[] importAgentDTOS = gson
                .fromJson(readAgentsFromFile(), ImportAgentDTO[].class);

        List<String> result = new ArrayList<>();

        for (ImportAgentDTO importAgentDTO : importAgentDTOS) {
            if (validationUtil.isValid(importAgentDTO)){
                Optional<Agent> optAgent = agentRepository.findAgentByFirstName(importAgentDTO.getFirstName());

                if(optAgent.isEmpty()) {
                    Agent agent = modelMapper.map(importAgentDTO, Agent.class);

                    Optional<Town> town = townRepository.findByTownName(importAgentDTO.getTown());

                    agent.setTown(town.get());

                    agentRepository.save(agent);

                    String msg = String.format("Successfully imported agent - %s %s",
                            agent.getFirstName(),
                            agent.getLastName());

                    result.add(msg);
                } else {
                    result.add("Invalid agent");
                }
            } else {
                result.add("Invalid agent");
            }
        }
        return String.join("\n", result);
    }
}
