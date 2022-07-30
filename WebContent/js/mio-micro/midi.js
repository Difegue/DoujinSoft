window.midiWriter = require('midi-writer-js');

const TRACK_COUNT = 4;
const TEMPO = 120;
const BASE_SONG_OFFSET = 0xB961;
const BASE_INSTRUMENT_OFFSET = 0xBA6B;
const BASE_VOLUME_OFFSET = 0xBA61;

const notes = {
	[0]: 'G3',
	[1]: 'G#3',
	[2]: 'A4',
	[3]: 'A#4',
	[4]: 'B4',
	[5]: 'C4',
	[6]: 'C#4',
	[7]: 'D4',
	[8]: 'D#4',
	[9]: 'E4',
	[10]: 'F4',
	[11]: 'F#4',
	[12]: 'G4',
	[13]: 'G#4',
	[14]: 'A5',
	[15]: 'A#5',
	[16]: 'B5',
	[17]: 'C5',
	[18]: 'C#5',
	[19]: 'D5',
	[20]: 'D#5',
	[21]: 'E5',
	[22]: 'F5',
	[23]: 'F#5',
	[24]: 'G5'
};
	
const lowerNotes = {
	[0]: 'G2',
	[1]: 'G#2',
	[2]: 'A3',
	[3]: 'A#3',
	[4]: 'B3',
	[5]: 'C3',
	[6]: 'C#3',
	[7]: 'D3',
	[8]: 'D#3',
	[9]: 'E3',
	[10]: 'F3',
	[11]: 'F#3',
	[12]: 'G3',
	[13]: 'G#3',
	[14]: 'A4',
	[15]: 'A#4',
	[16]: 'B4',
	[17]: 'C4',
	[18]: 'C#4',
	[19]: 'D4',
	[20]: 'D#4',
	[21]: 'E4',
	[22]: 'F4',
	[23]: 'F#4',
	[24]: 'G4'
};

const instrumentCodes = [0, 18, 6, 22, 73, 56, 65, 75, 24, 29, 106, 33, 40, 13, 11, 47, 72, 78, 17, 38, 77, 59,
						126, 124, 60, 61, 62, 123, 66, 125, 68, 122, 53, 54, 52, 49, 67, 121, 119, 48, 83, 84,
						85, 86, 87, 88, 89, 90];

const instrumentLengths = [
	6, 10, 4, 9, 8, 2, 4, 8,
	4, 16, 4, 4, 8, 2, 10, 3,
	20, 4.5, 7.5, 2.5, 4, 1, 7, 1.5,
	2, 1.5, 2, 2.5, 1, 3.5, 2, 2,
	3, 8, 8.5, 8.5, 3, 2, 2, 12,
	8, 8.5, 12, 8, 12, 5, 4, 2
];

const bassInstruments = [11, 43, 47];

window.buildMidiFile = (mioData) => {
	
	let MidiWriter = window.midiWriter

	let tracks = [];

	let trackLength = 32;
	for (let trackIndex = 0; trackIndex < TRACK_COUNT; trackIndex++) {
		let track = new MidiWriter.Track();

		track.setTempo(TEMPO);
		
		let songOffset = BASE_SONG_OFFSET + trackIndex * trackLength;
		
		let instrumentUsed = mioData[BASE_INSTRUMENT_OFFSET + trackIndex];
		let volume = mioData[BASE_VOLUME_OFFSET + trackIndex] * 20;

		track.addEvent(new MidiWriter.ProgramChangeEvent({instrument: parseInt(instrumentCodes[instrumentUsed])}));

		let notesUsed = notes;
		if (bassInstruments.includes(instrumentUsed)) {
			notesUsed = lowerNotes;
		}

		for (let i = 0; i < trackLength; i++) {
			let note = mioData[songOffset + i];
			if (note === 255) {
			} else {
				let duration = 'T' + (instrumentLengths[instrumentUsed] * 8);
				track.addEvent(new MidiWriter.NoteEvent({pitch: notesUsed[note],
							duration,
							startTick: 32 * i,
							velocity: volume}));
			}
		}
		
		tracks.push(track);
	}
	
	// TODO: Rhythm track

	let write = new MidiWriter.Writer(tracks);

	return write.buildFile()
}
