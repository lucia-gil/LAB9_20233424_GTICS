CREATE DATABASE IF NOT EXISTS stf_games_20233424 DEFAULT CHARACTER SET utf8mb4;
USE stf_games_20233424;
CREATE TABLE IF NOT EXISTS stf_puzzle_sv (
    stf_game_board_structure VARCHAR(20) NOT NULL,
    image LONGTEXT NOT NULL
);

DELETE FROM stf_puzzle_sv;
INSERT INTO stf_puzzle_sv (stf_game_board_structure, image) VALUES ('13652847X', '');
