/* Skeleton code copyright (C) 2008, 2022 Paul N. Hilfinger and the
 * Regents of the University of California.  Do not distribute this or any
 * derivative work without permission. */

package ataxx;

import ucb.gui2.Pad;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.BasicStroke;

import java.awt.event.MouseEvent;

import java.util.concurrent.ArrayBlockingQueue;

/** Widget for displaying an Ataxx board.
 *  @author Frank Jin
 */
class BoardWidget extends Pad  {

    /** Length of side of one square, in pixels. */
    static final int SQDIM = 50;
    /** Number of squares on a side. */
    static final int SIDE = Board.SIDE;
    /** Radius of circle representing a piece. */
    static final int PIECE_RADIUS = 15;
    /** Dimension of a block. */
    static final int BLOCK_WIDTH = 40;

    /** Color of red pieces. */
    private static final Color RED_COLOR = Color.RED;
    /** Color of blue pieces. */
    private static final Color BLUE_COLOR = Color.BLUE;
    /** Color of painted lines. */
    private static final Color LINE_COLOR = Color.BLACK;
    /** Color of blank squares. */
    private static final Color BLANK_COLOR = Color.WHITE;
    /** Color of selected squared. */
    private static final Color SELECTED_COLOR = new Color(150, 150, 150);
    /** Color of blocks. */
    private static final Color BLOCK_COLOR = Color.BLACK;

    /** Stroke for lines. */
    private static final BasicStroke LINE_STROKE = new BasicStroke(1.0f);
    /** Stroke for blocks. */
    private static final BasicStroke BLOCK_STROKE = new BasicStroke(5.0f);

    /** A new widget sending commands resulting from mouse clicks
     *  to COMMANDQUEUE. */
    BoardWidget(ArrayBlockingQueue<String> commandQueue) {
        _commandQueue = commandQueue;
        setMouseHandler("click", this::handleClick);
        _dim = SQDIM * SIDE;
        _blockMode = false;
        setPreferredSize(_dim, _dim);
        setMinimumSize(_dim, _dim);
    }

    /** Indicate that SQ (of the form CR) is selected, or that none is
     *  selected if SQ is null. */
    void selectSquare(String sq) {
        if (sq == null) {
            _selectedCol = _selectedRow = 0;
        } else {
            _selectedCol = sq.charAt(0);
            _selectedRow = sq.charAt(1);
        }
        repaint();
    }

    @Override
    public synchronized void paintComponent(Graphics2D g) {
        g.setColor(BLANK_COLOR);
        g.fillRect(0, 0, _dim, _dim);
        super.paintComponent(g);
        g.setColor(LINE_COLOR);
        g.setStroke(LINE_STROKE);
        for (int k = 0; k < 7; k++) {
            g.drawLine(0, k * SQDIM, _dim, k * SQDIM);
            g.drawLine(k * SQDIM, 0, k * SQDIM, _dim);
        }

        if (_selectedCol != 0 && _selectedRow != 0) {
            int selectedIndex = Board.index(_selectedCol, _selectedRow);
            g.setStroke(new BasicStroke(0));
            g.setColor(SELECTED_COLOR);
            g.fillRect(SQDIM * (selectedIndex % 11 - 2),
                    SQDIM * (7 - Math.floorDiv(selectedIndex, 11) + 1),
                    SQDIM, SQDIM);
        }

        for (char x = 'a'; x <= 'g'; x = (char) (x + 1)) {
            for (char y = '1'; y <= '7'; y = (char) (y + 1)) {
                int index = Board.index(x, y);
                if (_model.get(x, y) == PieceColor.BLOCKED) {
                    drawBlock(g, SQDIM * (index % 11) - _dim / 2,
                            SQDIM * Math.floorDiv(index, 11) + _dim / 2);
                } else if (_model.get(x, y) != PieceColor.EMPTY) {
                    if (_model.get(x, y) == PieceColor.BLUE) {
                        g.setColor(BLUE_COLOR);
                    } else {
                        g.setColor(RED_COLOR);
                    }
                    g.fillOval(SQDIM * (index % 11 - 2) + SQDIM / 2
                                - PIECE_RADIUS, SQDIM
                                * (7 - Math.floorDiv(index, 11) + 1)
                                + SQDIM / 2 - PIECE_RADIUS,
                            PIECE_RADIUS * 2, PIECE_RADIUS * 2);
                }
            }
        }
    }

    /** Draw a block centered at (CX, CY) on G. */
    void drawBlock(Graphics2D g, int cx, int cy) {
        int leftX = cx - BLOCK_WIDTH / 2;
        int topY = cy + BLOCK_WIDTH / 2;
        super.paintComponent(g);
        g.setColor(BLOCK_COLOR);
        g.setStroke(BLOCK_STROKE);
        g.drawRect(leftX, topY, BLOCK_WIDTH, BLOCK_WIDTH);
        g.drawLine(leftX, topY, leftX + BLOCK_WIDTH, topY - BLOCK_WIDTH);
        g.drawLine(leftX, topY - BLOCK_WIDTH, leftX + BLOCK_WIDTH, topY);
        g.drawLine(cx, cy - BLOCK_WIDTH / 2, cx, cy + BLOCK_WIDTH / 2);
        g.drawLine(cx - BLOCK_WIDTH / 2, cy, cx + BLOCK_WIDTH / 2, cy);
    }

    /** Clear selected block, if any, and turn off block mode. */
    void reset() {
        _selectedRow = _selectedCol = 0;
        setBlockMode(false);
    }

    /** Set block mode on iff ON. */
    void setBlockMode(boolean on) {
        _blockMode = on;
    }

    /** Issue move command indicated by mouse-click event WHERE. */
    private void handleClick(String unused, MouseEvent where) {
        int x = where.getX(), y = where.getY();
        char mouseCol, mouseRow;
        if (where.getButton() == MouseEvent.BUTTON1) {
            mouseCol = (char) (x / SQDIM + 'a');
            mouseRow = (char) ((SQDIM * SIDE - y) / SQDIM + '1');
            if (mouseCol >= 'a' && mouseCol <= 'g'
                && mouseRow >= '1' && mouseRow <= '7') {
                if (_blockMode) {
                    _commandQueue.offer("block " + mouseCol + mouseRow);
                } else {
                    if (_selectedCol != 0) {
                        if (_model.get(_selectedCol, _selectedRow)
                                == _model.whoseMove()
                                && _model.legalMove(_selectedCol, _selectedRow,
                                mouseCol, mouseRow)) {
                            _commandQueue.offer("" + _selectedCol
                                    + _selectedRow + "-" + mouseCol + mouseRow);
                        }
                        selectSquare("" + mouseCol + mouseRow);
                        _selectedCol = _selectedRow = 0;
                    } else {
                        _selectedCol = mouseCol;
                        _selectedRow = mouseRow;
                    }
                }
            }
        }
        repaint();
    }

    public synchronized void update(Board board) {
        _model = new Board(board);
        repaint();
    }

    /** Dimension of current drawing surface in pixels. */
    private int _dim;

    /** Model being displayed. */
    private static Board _model;

    /** Coordinates of currently selected square, or '\0' if no selection. */
    private char _selectedCol, _selectedRow;

    /** True iff in block mode. */
    private boolean _blockMode;

    /** Destination for commands derived from mouse clicks. */
    private ArrayBlockingQueue<String> _commandQueue;
}
