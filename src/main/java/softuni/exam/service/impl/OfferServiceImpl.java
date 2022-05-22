package softuni.exam.service.impl;

import com.google.gson.Gson;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import softuni.exam.models.dto.ImportOfferDTO;
import softuni.exam.models.dto.ImportOfferRootDTO;
import softuni.exam.models.entity.Agent;
import softuni.exam.models.entity.Apartment;
import softuni.exam.models.entity.ApartmentType;
import softuni.exam.models.entity.Offer;
import softuni.exam.repository.AgentRepository;
import softuni.exam.repository.ApartmentRepository;
import softuni.exam.repository.OfferRepository;
import softuni.exam.service.OfferService;
import softuni.exam.util.ValidationUtil;
import softuni.exam.util.XmlParser;

import javax.xml.bind.JAXBException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class OfferServiceImpl implements OfferService {
    private static final String OFFERS_FILE_PATH = "src/main/resources/files/xml/offers.xml";
    private final OfferRepository offerRepository;
    private final AgentRepository agentRepository;
    private final ApartmentRepository apartmentRepository;
    private final ValidationUtil validationUtil;
    private final ModelMapper modelMapper;
    private final XmlParser xmlParser;

    public OfferServiceImpl(OfferRepository offerRepository, AgentRepository agentRepository, ApartmentRepository apartmentRepository, Gson gson, ValidationUtil validationUtil, ModelMapper modelMapper, XmlParser xmlParser) {
        this.offerRepository = offerRepository;
        this.agentRepository = agentRepository;
        this.apartmentRepository = apartmentRepository;
        this.validationUtil = validationUtil;
        this.modelMapper = modelMapper;
        this.xmlParser = xmlParser;
    }

    @Override
    public boolean areImported() {
        return offerRepository.count() > 0;
    }

    @Override
    public String readOffersFileContent() throws IOException {
        return Files.readString(Path.of(OFFERS_FILE_PATH));
    }

    @Override
    public String importOffers() throws IOException, JAXBException {
        ImportOfferRootDTO importOfferRootDTO = xmlParser
                .fromFile(OFFERS_FILE_PATH, ImportOfferRootDTO.class);

        return importOfferRootDTO
                .getOffers()
                .stream()
                .map(this::importOffer)
                .collect(Collectors.joining("\n"));
    }

    @Override
    public String exportOffers() {
        StringBuilder sb = new StringBuilder();

        offerRepository.findThreeRoomApartmentsOrderByAreaDescAndPriceAsc(ApartmentType.three_rooms)
                .forEach(offer -> {
                    sb.append(String.format(
                            "Agent %s %s with offer №%d\n" +
                                    "\t-Apartment area: %.2f\n" +
                                    "\t--Town: %s\n" +
                                    "\t---Price: %.2f$\n",
                            offer.getAgent().getFirstName(),
                            offer.getAgent().getLastName(),
                            offer.getId(),
                            offer.getApartment().getArea(),
                            offer.getApartment().getTown().getTownName(),
                            offer.getPrice()
                    )).append(System.lineSeparator());
                });

        return sb.toString();
    }

    private String importOffer(ImportOfferDTO dto) {
        if(!validationUtil.isValid(dto)) {
            return "Invalid offer";
        }

        Optional<Agent> optAgent = agentRepository.findAgentByFirstName(dto.getAgent().getName());
        Optional<Apartment> optApartment = apartmentRepository.findById(dto.getApartment().getId());

        if(optAgent.isEmpty()) {
            return "Invalid offer";
        }

        Offer offer = modelMapper.map(dto, Offer.class);

        offer.setAgent(optAgent.get());
        offer.setApartment(optApartment.get());

        offerRepository.save(offer);

        return String.format("Successfully imported offer %.2f",
                offer.getPrice());
    }
}
