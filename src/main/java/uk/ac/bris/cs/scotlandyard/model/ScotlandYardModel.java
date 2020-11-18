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
import org.checkerframework.checker.units.qual.Current;
import uk.ac.bris.cs.gamekit.graph.Edge;
import uk.ac.bris.cs.gamekit.graph.Graph;
import uk.ac.bris.cs.gamekit.graph.ImmutableGraph;

import uk.ac.bris.cs.gamekit.graph.Graph;

// TODO implement all methods and pass all tests
public class ScotlandYardModel implements ScotlandYardGame, Consumer<Move>, MoveVisitor{
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

	//Generates all possible valid moves of the current player based on player's location
	private Set<Move> validMove(Colour player) {
		//Get the current player
		ScotlandYardPlayer currentPlayer = playerslist.get(CurrentPlayerIndex);
		//Get location of current player
		Integer location = currentPlayer.location();

		//Get all possible edges based on player's location and store it in a collection
		Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(graph.getNode(location));
		//Store all available location(nodes)
		List <Integer> nodes = new ArrayList<>();

		//Initialize a set of moves to store all possible moves
		Set<Move> moves = new HashSet<>();

		//Iterate through players and node
		for (Edge<Integer, Transport> edge : edges){
			for (ScotlandYardPlayer Player : playerslist){
				int destination = edge.destination().value();
				//check if any node is occupied by other player and if player has ticket
				if ( destination != Player.location() && currentPlayer.hasTickets(Ticket.fromTransport(edge.data())))
					//add the node to the list
					nodes.add(destination);
					moves.add(new TicketMove(currentPlayer.colour(), Ticket.fromTransport(edge.data()), destination));
			}
		}

		//store all possible second available edges for double move
		List<Edge<Integer, Transport>> secondEdges = new ArrayList<>();
		if(currentPlayer.isDetective() && currentPlayer.hasTickets(DOUBLE))
			for(Integer node : nodes) {
				//add all second available edges
				secondEdges.addAll(graph.getEdgesFrom(graph.getNode(node)));
			}

			for (Edge<Integer, Transport> edge : secondEdges){
				for(ScotlandYardPlayer Player : playerslist){
					int destination = edge.destination().value();
					if( destination != Player.location() && Player.hasTickets(Ticket.fromTransport(edge.data())))
						nodes.add(destination);
						moves.add(new TicketMove(currentPlayer.colour(), Ticket.fromTransport(edge.data()), destination ));
				}
			}

			if(!currentPlayer.hasTickets(Ticket.BUS) && !currentPlayer.hasTickets(Ticket.TAXI) && !currentPlayer.hasTickets(Ticket.UNDERGROUND))
				moves.add(new PassMove(currentPlayer.colour()));

			if(moves.isEmpty())
				moves.add(new PassMove(currentPlayer.colour()));

		return moves;
	}

	@Override
	public void startRotate() {
		//throw new RuntimeException("Implement me");
		if (isGameOver()) {
			throw new IllegalStateException("Game is over");
		}

		ScotlandYardPlayer currentPlayer = playerslist.get(CurrentPlayerIndex);
		Set <Move> moves = validMove(currentPlayer.colour());
		currentPlayer.player().makeMove(this, currentPlayer.location(), moves, this);
	}

	@Override
	public void accept(Move move) {
		requireNonNull(move);
		if (!validMove(move.colour()).contains(move)) {
			throw new IllegalArgumentException("Invalid Move");
		}
		move.visit(this);

		ScotlandYardPlayer currentPlayer = playerslist.get(CurrentPlayerIndex);

		if (!currentPlayer.isMrX() && !isGameOver()) {
			currentPlayer.player().makeMove(this, currentPlayer.location(), validMove(currentPlayer.colour()), this);
		}
	}

	@Override
	public Collection<Spectator> getSpectators() {
		// TODO
		throw new RuntimeException("Implement me");
	}

	//returns list of players(Colour)
	@Override
	public List<Colour> getPlayers() {
		ArrayList<Colour> colours = new ArrayList<>();
		for (ScotlandYardPlayer Player : playerslist) {
			colours.add(Player.colour());
		}
		return Collections.unmodifiableList(colours);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		return Collections.emptySet();
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		//If it is MrX, return MrX last known location
		if (colour == BLACK)
			return Optional.of(MrXLastLocation);
		else
			//return location of requested player
			for (ScotlandYardPlayer player : playerslist){
				if(player.colour() == colour)
					return Optional.of(player.location());
		}
		return Optional.empty();
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		for (ScotlandYardPlayer player : playerslist) {
			if (player.colour() == colour)
				return Optional.of(player.tickets().get(ticket));
		}
		return Optional.empty();
	}

	@Override
	public boolean isGameOver() {
		return false;
	}

	@Override
	public Colour getCurrentPlayer() {
		return playerslist.get(CurrentPlayerIndex).colour();
	}

	@Override
	public int getCurrentRound() {
		return CurrentRound;
	}

	@Override
	public List<Boolean> getRounds() {
		return Collections.unmodifiableList(rounds);
	}

	@Override
	public Graph<Integer, Transport> getGraph() {
		return new ImmutableGraph<>(graph);
	}

}
