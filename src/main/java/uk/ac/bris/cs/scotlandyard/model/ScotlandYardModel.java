package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.requireNonNull;
import static uk.ac.bris.cs.scotlandyard.model.Colour.BLACK;
import static uk.ac.bris.cs.scotlandyard.model.Ticket.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import javafx.collections.FXCollections;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

import uk.ac.bris.cs.gamekit.graph.Graph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame {
	private List<Boolean> rounds;
	private Graph<Integer, Transport> graph;
	private List<ScotlandYardPlayer> playerslist = new ArrayList<>();
	private int CurrentPlayerIndex = 0;
	private int CurrentRound = 0;
	private int MrXLastLocation = 0;

	public ScotlandYardModel(List<Boolean> rounds,
				Graph<Integer, Transport> graph,
				PlayerConfiguration mrX,
			 	PlayerConfiguration firstDetective,
				PlayerConfiguration... restOfTheDetectives) {

		ArrayList<PlayerConfiguration> configurations = new ArrayList<>();
		this.rounds = requireNonNull(rounds);
		this.graph = requireNonNull(graph);

		if (rounds.isEmpty()) {
			throw new IllegalArgumentException("Empty rounds");
		}

		if (graph.isEmpty()) {
			throw new IllegalArgumentException("Empty Map");
		}

		if (mrX.colour != BLACK) {
			throw new IllegalArgumentException("MrX should be Black");
		}

		for (PlayerConfiguration configuration : restOfTheDetectives)
			configurations.add(requireNonNull(configuration));
			configurations.add(0, firstDetective);
			configurations.add(0, mrX);

		//check if there is duplicated location
		Set<Integer> set = new HashSet<>();
		for (PlayerConfiguration configuration : configurations) {
			if (set.contains(configuration.location))
				throw new IllegalArgumentException("Duplicate location");
			set.add(configuration.location);
		}

		//check if there is duplicated colour
		Set<Colour> set2 = new HashSet<>();
		for (PlayerConfiguration configuration : configurations) {
			if (set2.contains(configuration.colour))
				throw new IllegalArgumentException("Duplicate colour");
			set2.add(configuration.colour);
		}

		//check for the correct ticket for MrX
		for(PlayerConfiguration configuration : configurations) {
			if (!configuration.tickets.keySet().contains(Ticket.BUS)
					|| !configuration.tickets.keySet().contains(Ticket.UNDERGROUND)
					|| !configuration.tickets.keySet().contains(Ticket.SECRET)
					|| !configuration.tickets.keySet().contains(Ticket.DOUBLE)
					|| !configuration.tickets.keySet().contains(Ticket.TAXI)) {
				throw new IllegalArgumentException("MrX should have all ticket types");
			}
		}

		//check if detectives have double or secret ticket
		for(int i=1; i < configurations.size(); i++) {
			if(configurations.get(i).tickets.get(Ticket.DOUBLE) != 0 ||
					configurations.get(i).tickets.get(Ticket.SECRET) != 0) {
				throw new IllegalArgumentException("Detectives should not have DOUBLE or SECRET ticket");
			}
		}

		for(PlayerConfiguration configuration : configurations){
			this.playerslist.add(new ScotlandYardPlayer(configuration.player,configuration.colour,
				configuration.location, configuration.tickets ));
		}
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		// TODO

		throw new RuntimeException("Implement me");
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public void startRotate() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public List<Colour> getPlayers() {
		// TODO
		ArrayList<Colour> colours = new ArrayList<>();
		for (ScotlandYardPlayer Player : playerslist) {
			colours.add(Player.colour());
		}
		return Collections.unmodifiableList(colours);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		// TODO
		return Collections.emptySet();
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		// TODO
		throw new RuntimeException("Implement me");
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		// TODO
		for (ScotlandYardPlayer player : playerslist) {
			if (player.colour() == colour)
				return Optional.of(player.tickets().get(ticket));
		}
		return Optional.empty();
	}

	@Override
	public boolean isGameOver() {
		// TODO
		return false;
	}

	@Override
	public Colour getCurrentPlayer() {
		// TODO
		return playerslist.get(CurrentPlayerIndex).colour();
	}

	@Override
	public int getCurrentRound() {
		// TODO
		return CurrentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		// TODO
		return Collections.unmodifiableList(rounds);

	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		// TODO
		return new ImmutableGraph<>(graph);
	}

}
