package com.difegue.doujinsoft.utils;

import com.xperia64.diyedit.Globals;
import com.xperia64.diyedit.editors.RecordEdit;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.sound.midi.MidiSystem;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;

import org.jfugue.MidiRenderer;
import org.jfugue.MusicStringParser;
import org.jfugue.Pattern;

public class ExportMidi
{
  private boolean forceSwing = false;
  private boolean forceUnSwing = false;
  private Logger MidiLog;
  
  private int offset = 43;
  private byte[] bb;
  
  public ExportMidi(byte[] b)
  {
    this.bb = b;
    
    MidiLog = Logger.getLogger("MidiExporter");
    MidiLog.addHandler(new StreamHandler(System.out, new SimpleFormatter()));   
  }
  
  public void export(String filename, boolean play)
  {
    String pl = calculatePlaystring();
    //MidiLog.log(Level.DEBU, "Music String for this MIO is: "+pl);
    
    File f = new File(filename);
    try
    {
      savePlayString(pl, f);
    }
    catch (IOException e)
    {
      e.printStackTrace();
    }
  }
  
  /*
   * Taken from jfugue's Player class.
   * dumps a musicString into a .midi file.
   */
  public void savePlayString(String musicString, File file) throws IOException
  {
	  Pattern p = new Pattern(musicString);

	  MusicStringParser parser = new MusicStringParser();
	  MidiRenderer renderer = new MidiRenderer(Sequence.PPQ, 120);
	  
	  renderer.reset();
      parser.addParserListener(renderer);
	  parser.parse(p);
      Sequence sequence = renderer.getSequence();

      int[] writers = MidiSystem.getMidiFileTypes(sequence);
      if (writers.length == 0) return;

      MidiSystem.write(sequence, writers[0], file);
  }


  
  /*
   * Calculate the midi string from the mio. 
   * Entirely taken from DIYEdit source code here, not much originality.
   */
  private String calculatePlaystring()
  {
    RecordEdit ed = new RecordEdit(this.bb);
    
    int t = ed.getTempo();
    if (((ed.getSwing()) || (this.forceSwing)) && (!this.forceUnSwing)) {
      t *= 3;
    } else {
      t *= 4;
    }
    StringBuilder s = new StringBuilder("T");
    s.append(t);
    
    boolean flipflop = false;
    for (int x = 0; x < 4; x++)
    {
      float dur = -1.0F;
      int lastNote = -1;
      for (int i = 0; i < ed.getFlag(); i++)
      {
        dur = 0.0F;
        
        int instrument = ed.getInstrument(i, x);
        float correctLength = Globals.instrumentLengths[instrument];
        int correctDecay = Globals.instrumentDecay[instrument];
        s.append(String.format(" V%d I%s X[Pan_Position]=%d X[Volume]=%d X72=%d", new Object[] { Integer.valueOf(x), Globals.instrumentConversion[instrument], Integer.valueOf(3200 * ed.getPanning(i, x)), Integer.valueOf(3200 * ed.getVolume(i, x)), Integer.valueOf(correctDecay) }));
        int correctPitch = Globals.instrumentOctave[instrument];
        for (int o = 0; o < 32; o++) {
          if (((ed.getSwing()) || (this.forceSwing)) && (!this.forceUnSwing))
          {
            if (flipflop)
            {
              flipflop = false;
              int note = ed.getNote(i, x, o);
              if (note != -1)
              {
                note += this.offset;
                dur = 0.0F;
                s.append(" [");
                s.append(note + correctPitch);
                s.append("]i");
                lastNote = s.length();
              }
              else if ((dur < correctLength) && (lastNote > 0))
              {
                s = new StringBuilder(s.substring(0, lastNote) + "i" + s.substring(lastNote, s.length()));
                
                dur += 0.5F;
                lastNote++;
              }
              else
              {
                s.append(" Ri");
              }
            }
            else
            {
              flipflop = true;
              int note = ed.getNote(i, x, o);
              if (note != -1)
              {
                note += this.offset;
                dur = 0.0F;
                s.append(" [");
                s.append(note + correctPitch);
                s.append("]q");
                lastNote = s.length();
              }
              else if ((dur < correctLength) && (lastNote > 0))
              {
                s = new StringBuilder(s.substring(0, lastNote) + "q" + s.substring(lastNote, s.length()));
                
                dur += 1.0F;
                lastNote++;
              }
              else
              {
                s.append(" Rq");
              }
            }
          }
          else
          {
            int note = ed.getNote(i, x, o);
            if (note != -1)
            {
              note += this.offset;
              dur = 0.0F;
              s.append(" [");
              s.append(note + correctPitch);
              s.append("]q");
              lastNote = s.length();
            }
            else if ((dur < correctLength) && (lastNote > 0))
            {
              dur += 1.0F;
              s = new StringBuilder(s.substring(0, lastNote) + "q" + s.substring(lastNote, s.length()));
              lastNote++;
            }
            else
            {
              s.append(" Rq");
            }
          }
        }
      }
    }
    flipflop = false;
    for (int i = 0; i < ed.getFlag(); i++) {
      for (int o = 0; o < 32; o++)
      {
        int drum1 = ed.getDrum(i, 0, o);
        int drum2 = ed.getDrum(i, 1, o);
        int drum3 = ed.getDrum(i, 2, o);
        int drum4 = ed.getDrum(i, 3, o);
        
        s.append(String.format(" V9 X[Pan_Position]=%d X[Volume]=%d ", new Object[] { Integer.valueOf(1200 * ed.getPanning(i, 4)), Integer.valueOf(2000 * ed.getVolume(i, 4)) }));
        if (((ed.getSwing()) || (this.forceSwing)) && (!this.forceUnSwing))
        {
          if (flipflop)
          {
            flipflop = false;
            if ((drum1 == -1) && (drum2 == -1) && (drum3 == -1) && (drum4 == -1))
            {
              s.append("Ri ");
            }
            else
            {
              if (drum1 != -1)
              {
                s.append(Globals.drumConversion[drum1]);
                if (drum2 == -1) {
                  s.append("i ");
                } else {
                  s.append("i+");
                }
              }
              if (drum2 != -1)
              {
                s.append(Globals.drumConversion[drum2]);
                if (drum3 == -1) {
                  s.append("i ");
                } else {
                  s.append("i+");
                }
              }
              if (drum3 != -1)
              {
                s.append(Globals.drumConversion[drum3]);
                if (drum4 == -1) {
                  s.append("i ");
                } else {
                  s.append("i+");
                }
              }
              if (drum4 != -1) {
                s.append(Globals.drumConversion[drum2] + "i ");
              }
            }
          }
          else
          {
            flipflop = true;
            if ((drum1 == -1) && (drum2 == -1) && (drum3 == -1) && (drum4 == -1))
            {
              s.append("Rq ");
            }
            else
            {
              if (drum1 != -1)
              {
                s.append(Globals.drumConversion[drum1]);
                if (drum2 == -1) {
                  s.append("q ");
                } else {
                  s.append("q+");
                }
              }
              if (drum2 != -1)
              {
                s.append(Globals.drumConversion[drum2]);
                if (drum3 == -1) {
                  s.append("q ");
                } else {
                  s.append("q+");
                }
              }
              if (drum3 != -1)
              {
                s.append(Globals.drumConversion[drum3]);
                if (drum4 == -1) {
                  s.append("q ");
                } else {
                  s.append("q+");
                }
              }
              if (drum4 != -1) {
                s.append(Globals.drumConversion[drum4] + "q ");
              }
            }
          }
        }
        else if ((drum1 == -1) && (drum2 == -1) && (drum3 == -1) && (drum4 == -1))
        {
          s.append("Rq ");
        }
        else
        {
          if (drum1 != -1)
          {
            s.append(Globals.drumConversion[drum1]);
            if (drum2 == -1) {
              s.append("q ");
            } else {
              s.append("q+");
            }
          }
          if (drum2 != -1)
          {
            s.append(Globals.drumConversion[drum2]);
            if (drum3 == -1) {
              s.append("q ");
            } else {
              s.append("q+");
            }
          }
          if (drum3 != -1)
          {
            s.append(Globals.drumConversion[drum3]);
            if (drum4 == -1) {
              s.append("q ");
            } else {
              s.append("q+");
            }
          }
          if (drum4 != -1) {
            s.append(Globals.drumConversion[drum4] + "q ");
          }
        }
      }
    }
    return s.toString();
  }
 
}
