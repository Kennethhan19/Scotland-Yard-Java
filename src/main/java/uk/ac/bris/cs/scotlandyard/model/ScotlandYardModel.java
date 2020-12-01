package uk.ac.bris.cs.scotlandyard.model;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonList;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Collections.unmodifiableList;
import static java.util.Collections.unmodifiableSet;
import static java.util.Objects.checkFromToIndex;
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
	private List<ScotlandYardPlayer> playerList = new ArrayList<>();
	private Set<Colour> winningPlayers = new HashSet<>();
	private int CurrentPlayerIndex;
	private int CurrentRound;
	private int MrXLastLocation;
	private List<Spectator> spectators = new ArrayList<>();

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
			this.playerList.add(new ScotlandYardPlayer(configuration.player,configuration.colour,
				configuration.location, configuration.tickets ));
		}

		CurrentPlayerIndex = 0;
		CurrentRound = NOT_STARTED;
		MrXLastLocation = 0;
	}

	@Override
	public void registerSpectator(Spectator spectator) {
		requireNonNull(spectator);
		if(spectators.contains(spectator)){
			throw new IllegalArgumentException("Spectator already registered");
		}
		spectators.add(spectator);
	}

	@Override
	public void unregisterSpectator(Spectator spectator) {
		requireNonNull(spectator);
		if (!spectators.contains(spectator)){
			throw new IllegalArgumentException("Unregistered spectator");
			}
		spectators.remove(spectator);
	}

	@Override
	public Collection<Spectator> getSpectators() {
		return Collections.unmodifiableList(spectators);
	}

	//check if the node is occupied by any detectives
	private boolean validNode(int destination) {
		boolean valid = true;
		for (ScotlandYardPlayer Player : playerList) {
			if (Player.location() == destination && Player.isDetective()) {
				//invalid node as node is occupied by detective
				valid = false;
			}
		}
		return valid;
	}

	//method to generate moves for a player
	private Set<Move> firstMove (ScotlandYardPlayer currentPlayer){
		//get location of currentPlayer
		int location = currentPlayer.location();

		//set to store all first moves
		Set<Move> firstMoves = new HashSet<>();

		//get all edges coming from the location (node)
		Collection<Edge<Integer, Transport>> edges = graph.getEdgesFrom(graph.getNode(location));

		//iterate through all edges
		for(Edge<Integer, Transport> edge : edges){
			//get destination of the edge
			int destination = edge.destination().value();
			// check if destination (node) is occupied
			if (validNode(destination)){
				//check if player has ticket to travel to destination(node)
				if (currentPlayer.hasTickets(Ticket.fromTransport(edge.data())))
					//generate moves for player
					firstMoves.add(new TicketMove(currentPlayer.colour(), Ticket.fromTransport(edge.data()), destination));
				// check if player has SECRET ticket (if player has secret ticket, player is MrX)
				if (currentPlayer.hasTickets(SECRET))
					//generates moves for player
					firstMoves.add(new TicketMove(currentPlayer.colour(), SECRET, destination));
			}
		}
		return firstMoves;
	}

	//Method to generate double move for MrX
	private Set<Move> doubleMove (ScotlandYardPlayer currentPlayer) {
		// store all doubleMoves
		Set<Move> doubleMoves = new HashSet<>();
		// get all firstMoves
		Set<Move> firstMoves = firstMove(currentPlayer);

		for (Move move : firstMoves) {
			// downcast each move
			TicketMove move1 = (TicketMove) move;
			//remove the ticket so it won't be considered when check for double move
			currentPlayer.removeTicket(move1.ticket());
			Set<Move> secondMoves = new HashSet<>();

			// get edges from firstMoves destination(node)
			Collection<Edge<Integer, Transport>> secondEdges = graph.getEdgesFrom(graph.getNode(move1.destination()));
			// iterate through each edge
			for (Edge<Integer, Transport> edge : secondEdges) {
				// get destination (node)of edge
				int destination = edge.destination().value();
				// check if occupied by detective
				if (validNode(destination)) {
					//check for appropriate ticket
					if (currentPlayer.hasTickets(Ticket.fromTransport(edge.data())))
						secondMoves.add(new TicketMove(currentPlayer.colour(), Ticket.fromTransport(edge.data()), destination));
					if (currentPlayer.hasTickets(SECRET))
						secondMoves.add(new TicketMove(currentPlayer.colour(), SECRET, destination));
				}
			}

			// add ticket back when done with checking
			currentPlayer.addTicket(move1.ticket());

			// iterate through all second moves
			for (Move move2 : secondMoves){
				// downcast
				TicketMove move_2 = (TicketMove) move2;
				// generate double moves
				doubleMoves.add(new DoubleMove(currentPlayer.colour(), move1, move_2));
			}
		}
		return doubleMoves;
	}

	//method to generate pass move
	private Set<Move> passMove (Set<Move> firstMoves, ScotlandYardPlayer currentPlayer){
		Set <Move> passMoves = new HashSet<>();
		// if detectives have no moves left, generate pass move
		if (firstMoves.isEmpty() && currentPlayer.isDetective()) {
			passMoves.add(new PassMove(currentPlayer.colour()));
		}
		return passMoves;
	}

	//Generates all possible valid moves of the current player based on player's location
	private Set<Move> validMove(ScotlandYardPlayer currentPlayer) {

		Set<Move> firstMoves = firstMove(currentPlayer);
		// stores all final validMove for the currentPlayer
		//add all first moves to final move set
		Set<Move> allMoves = new HashSet<>(firstMoves);


		//check if player (MrX) has DOUBLE ticket
		if(currentPlayer.hasTickets(DOUBLE)) {
			//check if this is last or second last round
			if (CurrentRound != getRounds().size() - 1 && CurrentRound != getRounds().size()){
				allMoves.addAll(doubleMove(currentPlayer));
			}
		}

		allMoves.addAll(passMove(firstMoves,currentPlayer));

		return allMoves;
	}

	@Override
	public void startRotate(){
		if (isGameOver()) {
			throw new IllegalStateException("Game is over");
		}

		ScotlandYardPlayer currentPlayer = playerList.get(CurrentPlayerIndex);
		Set <Move> moves = validMove(currentPlayer);
		currentPlayer.player().makeMove(this, currentPlayer.location(), moves, this);
	}

	@Override
	public void accept(Move move) {
		requireNonNull(move);

		//check if move is valid
		if (!validMove(playerList.get(CurrentPlayerIndex)).contains(move)) {
			throw new IllegalArgumentException("Invalid Move");
		}

		// update the currentPlayerIndex so next player will be called to make move
		CurrentPlayerIndex = (CurrentPlayerIndex + 1)%(playerList.size());
		//get next player
		ScotlandYardPlayer currentPlayer = playerList.get(CurrentPlayerIndex );
		//visit the move
		move.visit(this);

		//notify spectator if game is over
		if(isGameOver()){
			for (Spectator spectator : spectators){
				spectator.onGameOver(this, getWinningPlayers());
			}
		}

		//notify spectator when rotation completed
		if(currentPlayer.isMrX() && !isGameOver()){
			for (Spectator spectator : spectators){
				spectator.onRotationComplete(this);
			}
		}

		//Call makeMove to next player
		if (!currentPlayer.isMrX() && !isGameOver()) {
			currentPlayer.player().makeMove(this, currentPlayer.location(), validMove(currentPlayer), this);
		}
	}

	//on reveal round, update mr x last location to current location
	private void revealRound(int location){
		if(getRounds().get(CurrentRound)){
			MrXLastLocation = location;
		}
	}

	//previous player index
	private int previousPlayer (){
		int prevPlayerIndex;
		//if currentPlayer is 0, prev player is last player of list
		if (CurrentPlayerIndex == 0){
			prevPlayerIndex = playerList.size()-1;
		}
		else {prevPlayerIndex = CurrentPlayerIndex-1;}
		return prevPlayerIndex;
	}

	//implementation of visit method for ticket move
	@Override
	public void visit (TicketMove move) {
		ScotlandYardPlayer player = playerList.get(previousPlayer());

		if (player.isDetective()) {
			//update player location
			player.location(move.destination());
			//remove ticket used from player
			player.removeTicket(move.ticket());
			//add ticket to MrX
			playerList.get(0).addTicket(move.ticket());

			//notify spectator on move made
			for (Spectator spectator : spectators) {
				spectator.onMoveMade(this, move);
			}
		}

		if (player.isMrX()) {
			//update MrX location
			player.location(move.destination());
			//deal with reveal round
			revealRound(player.location());
			//Remove Location
			player.removeTicket(move.ticket());
			CurrentRound++;

			//notify spectator abt new round and move made
			for (Spectator spectator : spectators) {
				spectator.onRoundStarted(this, CurrentRound);
				if (!rounds.get(CurrentRound - 1)) {
					spectator.onMoveMade(this, new TicketMove(playerList.get(0).colour(), move.ticket(), MrXLastLocation));
				} else {
					spectator.onMoveMade(this, move);
				}
			}
		}
	}

	//implementation of visit method for double move
	@Override
	public void visit (DoubleMove move){
		requireNonNull(move);
		ScotlandYardPlayer MrX = playerList.get(0);

		//remove double ticket
		MrX.removeTicket(DOUBLE);

		int location1;
		int location2;
		//check if first move lies on reveal round
		Boolean r1 = getRounds().get(CurrentRound);
		//check if second move lies on reveal round
		Boolean r2 = getRounds().get(CurrentRound+1);
		//reveal - show loc else last loc
		if(r1){location1 = move.firstMove().destination();}
		else{location1 = MrXLastLocation;}
		if(r2){location2 = move.finalDestination();}
		else{location2 = MrXLastLocation;}
		//if first round is reveal round then last loc become first round location
		if(r1 && !r2){location2=location1;}
		DoubleMove doubleMove = new DoubleMove(MrX.colour(), move.firstMove().ticket(),location1, move.secondMove().ticket(), location2);
		//notify spectator of the move
		for(Spectator spectator: spectators){
			spectator.onMoveMade(this, doubleMove);
		}

		move.firstMove().visit(this);
		move.secondMove().visit(this);
	}

	//implementation of visit method for pass move
	@Override
	public void visit (PassMove move){
		for (Spectator spectator : spectators){
			spectator.onMoveMade(this, move);
		}
	}

	//returns list of players(Colour)
	@Override
	public List<Colour> getPlayers() {
		ArrayList<Colour> colours = new ArrayList<>();
		for (ScotlandYardPlayer Player : playerList) {
			colours.add(Player.colour());
		}
		return Collections.unmodifiableList(colours);
	}

	@Override
	public Set<Colour> getWinningPlayers() {
		if (isGameOver()) {
			return Collections.unmodifiableSet(winningPlayers);
		} else {
			return Collections.emptySet();
		}
	}

	@Override
	public Optional<Integer> getPlayerLocation(Colour colour) {
		//If it is MrX, return MrX last known location
		if (colour == BLACK)
			return Optional.of(MrXLastLocation);
		else
			//return location of requested player
			for (ScotlandYardPlayer player : playerList){
				if(player.colour() == colour)
					return Optional.of(player.location());
		}
		return Optional.empty();
	}

	@Override
	public Optional<Integer> getPlayerTickets(Colour colour, Ticket ticket) {
		for (ScotlandYardPlayer player : playerList) {
			if (player.colour() == colour)
				return Optional.of(player.tickets().get(ticket));
		}
		return Optional.empty();
	}

	//check if MrX is captured
	private boolean MrXCaptured (){
		for (ScotlandYardPlayer player : playerList){
			//check if player is detective and its location is same as MrX
			if (player.isDetective()) {
				if (player.location() == playerList.get(0).location()){
					return true;
				}
			}
		}
		return false;
	}

	//check if MrX cornered
	private boolean MrXCornered() {
		return (getCurrentPlayer() == BLACK && validMove(playerList.get(0)).isEmpty());
	}

	//Check if MrX is not captured in any round
	private boolean MrXNotCaptured(){
		return (CurrentRound == rounds.size() && getCurrentPlayer() == BLACK);
	}

	//check if detectives are stuck
	private boolean detectiveStuck() {
		boolean result = true;
		for (ScotlandYardPlayer player : playerList) {
			if (player.isDetective() ) {
				if (player.hasTickets(BUS) || player.hasTickets(TAXI) || player.hasTickets(UNDERGROUND)) {
					result = false;
				}
			}
		}
		return result;
	}

	@Override
	public boolean isGameOver() {
		boolean result = false;
		if(MrXCaptured() || MrXCornered()){
			for(ScotlandYardPlayer player : playerList)
				if (player.isDetective()) {
					winningPlayers.add(player.colour());
				}
			result = true;
		}

		if(MrXNotCaptured() || detectiveStuck()){
			winningPlayers.add(BLACK);
			result = true;
		}

		return result;
	}

	@Override
	public Colour getCurrentPlayer() {
		return playerList.get(CurrentPlayerIndex).colour();
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
