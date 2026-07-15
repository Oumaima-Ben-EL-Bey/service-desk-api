package de.oumaima.servicedesk.team;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TeamController {

    private final TeamRepository teamRepository;

    public TeamController(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    @GetMapping("/teams")
    public List<TeamResponse> teams() {
        return teamRepository.findAll().stream()
                .map(team -> new TeamResponse(
                        team.getId(),
                        team.getName()
                ))
                .toList();
    }
}
