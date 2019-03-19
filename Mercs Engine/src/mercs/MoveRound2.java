package mercs;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import main.Board;
import main.Move;
import main.RecordBasedMoveLogic;
import mercs.info.GameInfo;
import mercs.info.PieceInfo;
import mercs.info.PlayerInfo;



/**
 * Represents the available options for a round of movement for the current
 * player in a game of Mercs. The current player is able to look at the plays
 * that each of their pieces that are currently on the board can make. They can
 * also choose one of these pieces to make a specified play.
 * @author trevor
 */
public final class MoveRound2 {
	private final GameInfo info;


	/**
	 * @param info A game of Mercs that is currently in its move round.
	 */
	public MoveRound2(GameInfo info) {
		if(info.order().turn().playState() != PlayState.MOVE) {
			throw new IllegalArgumentException(
				"info does not describe a game of Mercs in its move round."
			);
		}
		this.info = info;
	}


	/**
	 * @return A mapping of each piece of the current player's that could make
	 * a play to all the plays that the piece could make.
	 */
	public Map<Integer, Move[][]> pieceToPlays() {
		/**
		 * Gets the current player's pieces in order to get those pieces'
		 * plays.
		 */
		Set<Integer> piecesOfCurrentPlayer = info
			.playerToInfo()
			.get(info.order().turn().currentPlayer())
			.pieces();

		/**
		 * Removes every piece of the current player's that isn't on the board,
		 * since those pieces cannot make plays.
		 */
		Iterator<Integer> piecesIter = piecesOfCurrentPlayer.iterator();
		while(piecesIter.hasNext()) {
			if(info.board().tileForPiece(piecesIter.next()) == null) {
				piecesIter.remove();
			}
		}

		//Gets the plays for each piece and maps them to that piece.
		Map<Integer, Move[][]> pieceToPlays = new HashMap<>();
		for(Integer piece : piecesOfCurrentPlayer) {
			Move[][] playsForPiece = info
				.pieceToInfo().get(piece)
				.logic().plays();
			pieceToPlays.put(piece, playsForPiece);
		}
		return pieceToPlays;
	}


	/**
	 * @param piece A piece of the current player's that is on the board.
	 * @param playIndex An index that points to a play in the array of plays
	 * that the given piece can make.
	 * @return The game of Mercs after the current player decides to make the
	 * given play.
	 */
	public GameInfo play(Integer piece, int playIndex) {
		Move[] play = pieceToPlays().get(piece)[playIndex];

		Board board = newBoard(play);
		Map<Integer, PieceInfo> pieceToInfo = newPieceToInfo(play);
		Map<Integer, PlayerInfo> playerToInfo = newPlayerToInfo(play);
	}


	/**
	 * @param play A play being made on the board.
	 * @return The board after the given play is made on it.
	 */
	private Board newBoard(Move[] play) {
		Board board = info.board();
		for(Move move : play) {
			board = board.makeMove(move);
		}
		return board;
	}


	/**
	 * @param play A play being made that could affect the pieces in the game.
	 * @return A mapping of each piece to it's information after play is made.
	 */
	private Map<Integer, PieceInfo> newPieceToInfo(Move[] play) {
		/**
		 * Maps each piece to its information, changing its logic to an updated
		 * version.
		 */
		Map<Integer, PieceInfo> pieceToInfo = new HashMap<>();
		for(Integer piece : info.pieceToInfo().keySet()) {
			//Reuses the type for each piece.
			PieceType type = info.pieceToInfo().get(piece).type();

			//Updates the logic for each piece.
			RecordBasedMoveLogic logic = info
				.pieceToInfo().get(piece)
				.logic().update(info.board(), play);

			PieceInfo pieceInfo = new PieceInfo(type, logic);
			pieceToInfo.put(piece, pieceInfo);
		}

		return pieceToInfo;
	}


	/**
	 * @param play A play being made (by the current player) that could affect
	 * the total number of pieces that the current player has captured.
	 * @return A mapping of each player to their information after play is
	 * made.
	 */
	private Map<Integer, PlayerInfo> newPlayerToInfo(Move[] play) {
		Map<Integer, PlayerInfo> playerToInfo = info.playerToInfo();
		Integer currentPlayer = info.order().turn().currentPlayer();

		/**
		 * Calculates the number of pieces that the current player captured
		 * during the play. For a piece to be "captured" by a player, it has to
		 * be taken off the board and the piece being taken off cannot belong
		 * to that player.
		 */
		int numPiecesCapturedDuringPlay = 0;
		for(Move move : play) {
			if(
				move.newPosition() == null
				&& !info
					.playerToInfo().get(currentPlayer)
					.pieces().contains(move.piece())
			) {
				numPiecesCapturedDuringPlay++;
			}
		}

		//Adds the pieces that current player captured to their total.
		PlayerInfo currentPlayerInfo = info.playerToInfo().get(currentPlayer);
		currentPlayerInfo = new PlayerInfo(
			currentPlayerInfo.pieces(),
			currentPlayerInfo.numPiecesCaptured()
				+ numPiecesCapturedDuringPlay,
			currentPlayerInfo.cooldown()
		);

		playerToInfo.put(currentPlayer, currentPlayerInfo);
		return playerToInfo;
	}
}