const WIN_CONDITIONS_COUNT = 6;
const SWITCH_CONDITIONS_COUNT = 6;
const OBJECT_COUNT = 15;
const OBJECT_ART_COUNT = 4;
const ART_BANK_COUNT = 4;
const INSTRUCTION_COUNT = 6;
const TRIGGER_COUNT = 6;
const ACTION_COUNT = 6;

const BACKGROUND_OFFSET = 0x100;
const BACKGROUND_LENGTH = 0x30FF - BACKGROUND_OFFSET;
const LAYER_OFFSET = 0xE5F6;

const Switch = {
	On: 'On',
	Off: 'Off'
};

const AnimationStyle = {
	Hold: 'Hold',
	PlayOnce: 'PlayOnce',
	Loop: 'Loop'
};

const animationStyleMap = {
	[0]: AnimationStyle.Hold,
	[1]: AnimationStyle.PlayOnce,
	[2]: AnimationStyle.Loop
};

function styleFromNumber(digit) {
	return animationStyleMap[digit];
}

const Speed = {
	Slowest: 'Slowest',
	Slow: 'Slow',
	Normal: 'Normal',
	Fast: 'Fast',
	Fastest: 'Fastest'
};

const speedMap = {
	[0]: Speed.Slowest,
	[1]: Speed.Slow,
	[2]: Speed.Normal,
	[3]: Speed.Fast,
	[4]: Speed.Fastest
};

function speedFromNumber(digit) {
	return speedMap[digit];
}

const Overlap = {
	Anywhere: 'Anywhere',
	TryNotToOverlap: 'TryNotToOverlap'
};

const Time = {
	End: 'End'
};

const TouchesWhat = {
	Location: 'Location',
	AnotherObject: 'AnotherObject'
};

const ContactType = {
	Touch: 'Touch',
	Overlap: 'Overlap',
};

const SwitchWhen = {
	TurnsOn: 'TurnsOn',
	IsOn: 'IsOn',
	TurnsOff: 'TurnsOff',
	IsOff: 'IsOff'
};

const switchWhenMap = {
	[0]: SwitchWhen.TurnsOn,
	[1]: SwitchWhen.IsOn,
	[2]: SwitchWhen.TurnsOff,
	[3]: SwitchWhen.IsOff,
};

function switchWhenFromNumber(digit) {
    let switchWhen = switchWhenMap[digit];
    if (switchWhen === undefined) {
        console.warn('Unknown SwitchWhen');
    }
    return switchWhen;
}

const GameCondition = {
	Win: 'Win',
	Loss: 'Loss',
	HasBeenWon: 'HasBeenWon',
	HasBeenLost: 'HasBeenLost',
	NotYetWon: 'NotYetWon',
	NotYetLost: 'NotYetLost'
};

const gameConditionMap = {
	[0]: GameCondition.Win,
	[1]: GameCondition.Loss,
	[2]: GameCondition.HasBeenWon,
	[3]: GameCondition.HasBeenLost,
    [4]: GameCondition.NotYetWon,
    [5]: GameCondition.NotYetLost,
};

function gameConditionFromNumber(digit) {
    let gameCondition = gameConditionMap[digit];
    if (gameCondition === undefined) {
        console.warn('Unknown GameCondition');
    }
    return gameCondition;
}

const Trigger = {
	TapThisObject: 'TapThisObject',
	TapAnywhere: 'TapAnywhere',
	TimeExact: 'TimeExact',
	TimeRandom: 'TimeRandom',
	Contact: 'Contact',
	Switch: 'Switch',
	SpecificArt: 'SpecificArt',
	FinishesPlaying: 'FinishesPlaying',
	GameCondition: 'GameCondition'
};

const Action = {
	Travel: 'Travel',
	Switch: 'Switch',
	Lose: 'Lose',
	ChangeArt: 'ChangeArt',
	StopPlaying: 'StopPlaying',
	SoundEffect: 'SoundEffect',
	ScreenEffect: 'ScreenEffect'
};

const FromLocation = {
	Current: 'Current',
	AnotherPosition: 'AnotherPosition',
	AnotherObject: 'AnotherObject',
};

const Direction = {
	Random: 'Random',
	Location: 'Location',
	Specific: 'Specific'
};

const SpecificDirection = {
	North: 'North',
	NorthEast: 'NorthEast',
	East: 'East',
	SouthEast: 'SouthEast',
	South: 'South',
	SouthWest: 'SouthWest',
	West: 'West',
	NorthWest: 'NorthWest'
};

const specificDirectionMap = {
	[0]: SpecificDirection.North,
	[1]: SpecificDirection.NorthEast,
	[2]: SpecificDirection.East,
	[3]: SpecificDirection.SouthEast,
    [4]: SpecificDirection.South,
    [5]: SpecificDirection.SouthWest,
    [6]: SpecificDirection.West,
    [7]: SpecificDirection.NorthWest,
};

function specificDirectionFromNumber(digit) {
    let direction = specificDirectionMap[digit];
    if (direction === undefined) {
        console.warn('Unknown SpecificDirection');
    }
    return direction;
}

const Travel = {
	GoStraight: 'GoStraight',
	Stop: 'Stop',
	JumpToPosition: 'JumpToPosition',
	JumpToArea: 'JumpToArea',
	JumpToObject: 'JumpToObject',
	Swap: 'Swap',
	Roam: 'Roam',
	Target: 'Target'
};

const Roam = {
	Wiggle: 'Wiggle',
	Insect: 'Insect',
	Reflect: 'Reflect',
	Bounce: 'Bounce'
};

const roamMap = {
	[0]: Roam.Wiggle,
	[1]: Roam.Insect,
	[2]: Roam.Reflect,
	[3]: Roam.Bounce,
};

function roamFromNumber(digit) {
    let roam = roamMap[digit];
    if (roam === undefined) {
        console.warn('Unknown Roam');
    }
    return roam;
}

const ScreenEffect = {
	Flash: 'Flash',
	Shake: 'Shake',
	Confetti: 'Confetti',
	Freeze: 'Freeze'
};

const Length = {
	Short: 'Short',
	Long: 'Long',
	Boss: 'Boss'
};

const StartLocation = {
	Position: 'Position',
	Area: 'Area',
	AttachToObject: 'AttachToObject'
};

function sliceFrom(data, offset, length) {
	return data.slice(offset, offset + length);
}

function nameFromData(data, offset, length) {
	let slice = sliceFrom(data, offset, length);
	let nullTerminatorIndex = slice.findIndex(e => e == 0);
	if (nullTerminatorIndex !== -1) {
		length = nullTerminatorIndex;
		slice = sliceFrom(data, offset, length);
	}

	return String.fromCharCode(...slice);
}

function firstHexDigit(_byte) {
	return _byte >> 4;
}

function secondHexDigit(_byte) {
	return _byte & 0x0F;
}

let hasBitsSet = (data, bits) => {
	return (data & bits) === bits;
};

class GameData {
	constructor(data) {
		this.data = data;
	}

	get name() {
		let offset = 0x001C;
		let length = 20;
		return nameFromData(this.data, offset, length);
	}

	get command() {
		let offset = 0xE5DD;
		let length = 18;
		return nameFromData(this.data, offset, length);
	}

	get length() {
		if (secondHexDigit(this.data[0xE605]) === 0) {
			return Length.Short;
		} else if (secondHexDigit(this.data[0xE605]) === 1) {
			return Length.Long;
		} else {
			return Length.Boss;
		}
	}

	object(index) {
		return new ObjectData(this.data, index);
	}

	winCondition(conditionIndex, switchIndex) {
		let offset = 0xE5B9 + conditionIndex * 6 + switchIndex;
        let switchMap = {
            [1]: Switch.On,
            [2]: Switch.Off
        };
        let switchState = switchMap[secondHexDigit(this.data[offset])];
        if (switchState === undefined) {
            return null;
        }
		let index = firstHexDigit(this.data[offset]);
		return { index, switchState };
	}
}

function objectOffset(index) {
	return 0xB100 + index * 0x88;
}

function assemblyOffset(index) {
	return 0xBBB9 + index * 720;
}

class ObjectData {
	constructor(data, index) {
		this.data = data;
		this.index = index;
	}

	get offset() {
		return objectOffset(this.index);
	}

	get isActive() {
		let offset = this.offset + 5;
		return this.data[offset] === 0x01;
	}

	get name() {
		let offset = this.offset + 6;
		let length = 18;
		return nameFromData(this.data, offset, length);
	}

	get spriteSize() {
		let offset = this.offset + 4;
		return 16 * (this.data[offset] + 1);
	}

	art(index) {
		return new ArtData(this.data, this, index);
	}

	get assembly() {
		return new AssemblyData(this.data, this);
	}
}


class ArtData {
	constructor(data, object, index) {
		this.data = data;
		this.object = object;
		this.index = index;
	}

	get offset() {
		return this.object.offset + 0x19 + this.index * 0x1C;
	}

	get isActive() {
		let offset = this.offset;
		let _byte = this.data[offset];
		let hasValidFirstByte = _byte <= 0x04;
		return hasValidFirstByte && this.count !== 0;
	}

	get count() {
		let count = this.data[this.offset + 1];
		return count;
	}

	get name() {
		let offset = this.offset + 6;
		let length = 18;
		return nameFromData(this.data, offset, length);
	}

	get bank() {
		let offset = this.offset + 2;
		let length = this.count;
		return sliceFrom(this.data, offset, length);
	}
}

class AssemblyData {
	constructor(data, object) {
		this.data = data;
		this.object = object;
	}

	get offset() {
		return assemblyOffset(this.object.index);
	}

	get isActive() {
		let offset = this.offset;
		return this.data[offset] === 0x04;
	}

	get startInstruction() {
		let art = this.startArt;
        let location = this.startLocation;

		return { art, location };
	}

    get startArt() {
        let offset = this.offset;

        let index = firstHexDigit(this.data[offset + 1]);
		let style = styleFromNumber(firstHexDigit(this.data[offset + 2]));
		let speed = speedFromNumber(firstHexDigit(this.data[offset + 3]));
		let art = {
			index,
			style,
			speed
		};

        return art;
    }

    get startLocation() {
        let offset = this.offset;

        let positionSlice = sliceFrom(this.data, offset + 14, 8);

		let x = positionFromScrambledData(positionSlice, 'GFEDCBA-	-----!IH');
		let y = positionFromScrambledData(positionSlice, '--------	EDCBA---	---!IHGF');
		let position = { x, y };

		if (this.data[offset + 12] === 0x07) {
			return { tag: StartLocation.Position, position };
		} else if (this.data[offset + 12] === 0x87) {
			let overlap;
			if (this.data[offset + 13] === 0x04) {
				overlap = Overlap.TryNotToOverlap;
			} else {
				overlap = Overlap.Anywhere;
			}
			let positionSlice = sliceFrom(this.data, offset + 16, 4);

			let x = positionFromScrambledData(positionSlice, '-----CBA	-!IHGFED');
			let y = positionFromScrambledData(positionSlice, '--------	A-------	IHGFEDCB	-------!');
			let min = position;
			let max = { x, y };
			return { tag: StartLocation.Area, area: { min, max }, overlap };
		} else if ((this.data[offset + 12] & 0x10) !== 0) {
			let index = (this.data[offset + 13] >> 5) + (this.data[offset + 14] % 2) * 8;
			let _offset = {
				x: position.x - 96,
				y: position.y - 64
			};
			return { tag: StartLocation.AttachToObject, index, offset: _offset }
		} else {
			console.warn('Unknown start location type');
            return null;
		}
    }

	instruction(index) {
		return new InstructionData(this.data, this, index);
	}
}

/*
    unscambleInstructions is a tab separated string of bit -> number mappings
    For example you might pass in 'GFEDCBA_   -----!IH'
    where { A, B, C, D, E, F, G, H, I, ! } represent { 1, 2, 4, 8, 16, 32, 64, 128, 256, -512 } if they are set

*/
function positionFromScrambledData(data, unscrambleInstructions) {
	let position = 0;
	let splitInstructions = unscrambleInstructions.split('\t');
	for (let byteIndex = 0; byteIndex < splitInstructions.length; byteIndex++) {
		for (let bitIndex = 0; bitIndex < splitInstructions[byteIndex].length; bitIndex++) {
			let isBitSet = (data[byteIndex] & (1 << (7 - bitIndex))) !== 0;
			if (!isBitSet) {
				continue;
			}
			switch (splitInstructions[byteIndex][bitIndex]) {
				case '!': {
					position -= 512;
					break;
				}
				case 'I': {
					position += 256;
					break;
				}
				case 'H': {
					position += 128;
					break;
				}
				case 'G': {
					position += 64;
					break;
				}
				case 'F': {
					position += 32;
					break;
				}
				case 'E': {
					position += 16;
					break;
				}
				case 'D': {
					position += 8;
					break;
				}
				case 'C': {
					position += 4;
					break;
				}
				case 'B': {
					position += 2;
					break;
				}
				case 'A': {
					position += 1;
					break;
				}
			}
		}
	}

	return position;
}

class InstructionData {
	constructor(data, assembly, index) {
		this.data = data;
		this.assembly = assembly;
		this.index = index;
	}

	get offset() {
		return this.assembly.offset + 72 + this.index * 120;
	}

	get isActive() {
		return this.trigger(0) !== null;
	}

	trigger(index) {
		let length = 8;
		let offset = this.offset + index * length;

		let triggerSlice = sliceFrom(this.data, offset, length);
		let triggerTag = triggerSlice[0];

		let timeFromData = (data, offset) => {
			return firstHexDigit(data[offset]) + (secondHexDigit(data[offset + 1]) & 0x0F) * 16;
		};

		if (triggerTag === 0x11) {
			return { tag: Trigger.TapAnywhere };
		} else if (secondHexDigit(triggerTag) === 0x01) {
			return { tag: Trigger.TapThisObject };
		} else if (triggerTag === 0x02) {
			let time;
			if (this.data[offset + 2] === 0x14) {
				time = Time.End;
			} else {
				time = timeFromData(this.data, offset + 1);
			}
			return { tag: Trigger.TimeExact, when: time };
		} else if (triggerTag === 0x12) {
			let start = timeFromData(this.data, offset + 1);
			let end;
			if (secondHexDigit(this.data[offset + 3]) === 0x02) {
				end = Time.End;
			} else {
				end = timeFromData(this.data, offset + 2);
			}
			return { tag: Trigger.TimeRandom, start, end };
		} else if (secondHexDigit(triggerTag) === 0x03) {
			let contact;
			if ([0x13, 0x53, 0x93, 0xD3].includes(this.data[offset])) {
				contact = ContactType.Overlap;
			} else {
				contact = ContactType.Touch;
			}
			let touches;
			if (hasBitsSet(this.data[offset + 6], 0x04)) {
				let positionSlice = sliceFrom(this.data, offset + 1, 3);
				let x = positionFromScrambledData(positionSlice, 'FEDCBA--	----!IHG');
				let y = positionFromScrambledData(positionSlice, '--------	DCBA----	--!IHGFE');
				let min = { x, y };
				positionSlice = sliceFrom(this.data, offset + 3, 4);
				x = positionFromScrambledData(positionSlice, 'BA------	!IHGFEDC');
				y = positionFromScrambledData(positionSlice, '--------	--------	HGFEDCBA	------!I');
				let max = { x, y };
				touches = { what: TouchesWhat.Location, area: { min, max } };
			} else {
				let index = (this.data[offset] >> 6) + (this.data[offset + 1] % 4) * 4
				touches = { what: TouchesWhat.AnotherObject, index };
			}
			return { tag: Trigger.Contact, contact, touches };
		} else if (secondHexDigit(triggerTag) === 0x04) {
			let index = firstHexDigit(this.data[offset + 1]);
			let switchWhen = switchWhenFromNumber(this.data[offset + 2]);
			return { tag: Trigger.Switch, index, switchWhen };
		} else if (triggerTag === 0x05) {
			let index = firstHexDigit(this.data[offset + 1]);
			return { tag: Trigger.SpecificArt, index };
		} else if (triggerTag === 0x15) {
			return { tag: Trigger.FinishesPlaying };
		} else if (secondHexDigit(triggerTag) === 0x06) {
			let digit = firstHexDigit(triggerTag);
			let condition = gameConditionFromNumber(digit);
			return { tag: Trigger.GameCondition, condition };
		} else {
			return null;
		}
	}

	action(index) {
		let length = 12;
		let offset = this.offset + 48 + index * length;

		let actionSlice = sliceFrom(this.data, offset, length);
		let actionTag = actionSlice[0];

		let commonPosition = (data, offset) => {
			let positionSlice = sliceFrom(data, offset, 4);
			let x = positionFromScrambledData(positionSlice, 'BA000000	!IHGFEDC');
			let y = positionFromScrambledData(positionSlice, '--------	--------	HGFEDCBA	------!I');
			return { x, y };
		};

		if (actionTag === 0x01) {
			let speed;
			if (this.data[offset + 8] === 1) {
				speed = Speed.Fastest;
			} else {
				let speedDigit = this.data[offset + 7] >> 6;
				speed = speedFromNumber(speedDigit);
			}

			let positionSlice = sliceFrom(this.data, offset + 2, 3);
			let x = positionFromScrambledData(positionSlice, 'HGFEDCBA	------!I');
			let y = positionFromScrambledData(positionSlice, '--------	FEDCBA--	----!IHG');
			let position = { x, y };

			let from;
			if ((secondHexDigit(this.data[offset + 1]) & 0x00000111) === 0) {
				from = { tag: FromLocation.Current };
			} else if (secondHexDigit(this.data[offset + 1]) === 1) {
				from = { tag: FromLocation.AnotherPosition, position };
			} else if (hasBitsSet(this.data[offset + 1], 5)) {
				let index = firstHexDigit(this.data[offset + 1]);
				from = { tag: FromLocation.AnotherObject, index, offset: offsetFromPosition(position) };
			} else {
				console.warn('Unreachable FromLocation', this.data[offset + 1]);
			}

			let directionDigit = this.data[offset + 4];
			let direction;

			if (directionDigit === 0x10) {
				direction = { tag: Direction.Random };
			} else if (hasBitsSet(directionDigit, 0x20)) {
				let position = commonPosition(this.data, offset + 4);

				direction = { tag: Direction.Location, position };
			} else {
				let directionDigit = Math.floor((this.data[offset + 7] % 0x40) / 4);
				let dir = specificDirectionFromNumber(directionDigit);
				direction = { tag: Direction.Specific, direction: dir };
			}
			return { tag: Action.Travel, travel: Travel.GoStraight, from, direction, speed };
		} else if (actionTag === 0x11) {
			return { tag: Action.Travel, travel: Travel.Stop };
		} else if (actionTag === 0x21) {
			let moveToDigit = this.data[offset + 1] & 0x00011111;

			let position = commonPosition(this.data, offset + 2);

			if (moveToDigit === 0x00) {
				return { tag: Action.Travel, travel: Travel.JumpToPosition, position };
			} else if (secondHexDigit(moveToDigit) === 0x01) {
				let index = (this.data[offset + 2] >> 2) & 0x0F;
				return { tag: Action.Travel, travel: Travel.JumpToObject, index, offset: offsetFromPosition(position) };
			} else {
				let positionSlice = sliceFrom(this.data, offset + 5, 3);
				let x = positionFromScrambledData(positionSlice, 'FEDCBA--	----!IHG');
				let y = positionFromScrambledData(positionSlice, '--------	DCBA----	--!IHGFE');
				let max = { x, y };
				let area = { min: position, max };
				let overlap = firstHexDigit(this.data[offset + 1]) === 1 ? Overlap.Anywhere : Overlap.TryNotToOverlap;
				return { tag: Action.Travel, travel: Travel.JumpToArea, area, overlap };
			}
		} else if (actionTag === 0x31) {
			let index = secondHexDigit(this.data[offset + 1]);
			return { tag: Action.Travel, travel: Travel.Swap, index };
		} else if (actionTag === 0x41) {
			let positionSlice = sliceFrom(this.data, offset + 1, 4);
			let x = positionFromScrambledData(positionSlice, 'A-------	IHGFEDCB	-------!');
			let y = positionFromScrambledData(positionSlice, '--------	--------	GFEDCBA-	-----!IH');
			let min = { x, y };
			positionSlice = sliceFrom(this.data, offset + 4, 3);
			x = positionFromScrambledData(positionSlice, 'EDBCA---	---!IHGF');
			y = positionFromScrambledData(positionSlice, '--------	CBA-----	-!IHGFED');
			let max = { x, y };
			let area = { min, max };
            
			let roamTypeDigit = this.data[offset + 1] % 4;
			let roam = roamFromNumber(roamTypeDigit);

			let overlap;
			if (hasBitsSet(this.data[offset + 1], 0x10)) {
				overlap = Overlap.TryNotToOverlap;
			} else {
				overlap = Overlap.Anywhere;
			}

			let speedDigit = (this.data[offset + 6] >> 7) + (this.data[offset + 7] & 0x03) * 2;
			let speed = speedFromNumber(speedDigit);
			return { tag: Action.Travel, travel: Travel.Roam, roam, area, overlap, speed };
		} else if (actionTag === 0x51) {
			let index = secondHexDigit(this.data[offset + 1]);
			let positionSlice = sliceFrom(this.data, offset + 1, 3);
			let x = positionFromScrambledData(positionSlice, 'DCBA----	--!IHGFE');
			let y = positionFromScrambledData(positionSlice, '--------	BA------	!IHGFEDC');
			let position = { x, y };
			let speedDigit = this.data[offset + 4] & 0b00000111;
			let speed = speedFromNumber(speedDigit);
			return { tag: Action.Travel, travel: Travel.Target, index, offset: offsetFromPosition(position), speed };
		} else if (actionTag === 0x02) {
			return { tag: Action.Switch, switchTo: Switch.On };
		} else if (actionTag === 0x12) {
			return { tag: Action.Switch, switchTo: Switch.Off };
		} else if (secondHexDigit(actionTag) === 0x03) {
			return { tag: Action.Lose };
		} else if (actionTag === 0x04) {
			let index = firstHexDigit(this.data[offset + 1]);
			let style = styleFromNumber(firstHexDigit(this.data[offset + 2]));
			let speed = speedFromNumber(firstHexDigit(this.data[offset + 3]));
			return { tag: Action.ChangeArt, index, style, speed };
		} else if (actionTag === 0x14) {
			return { tag: Action.StopPlaying };
		}
		else if (secondHexDigit(this.data[offset]) === 5) {
			let effect = firstHexDigit(this.data[offset]) * 8 + firstHexDigit(this.data[offset + 1]);
			return { tag: Action.SoundEffect, effect };
		} else if (actionTag === 0x06) {
			return { tag: Action.ScreenEffect, effect: ScreenEffect.Flash };
		} else if (actionTag === 0x16) {
			return { tag: Action.ScreenEffect, effect: ScreenEffect.Shake };
		} else if (actionTag === 0x26) {
			return { tag: Action.ScreenEffect, effect: ScreenEffect.Confetti };
		} else if (actionTag === 0x36) {
			return { tag: Action.ScreenEffect, effect: ScreenEffect.Freeze };
		} else {
			return null;
		}
	}
}

function offsetFromPosition(position) {
	return { x: position.x - 96, y: position.y - 64 };
}
