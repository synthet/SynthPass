//    IAIK SHA3 Provider, a Java-library containing SHA3 candidate implementations  
//    Copyright (C) 2012 Stiftung Secure Information and Communication Technologies SIC 
//                       http://jce.iaik.tugraz.at
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    This program is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with this program.  If not, see <http://www.gnu.org/licenses/>.
package iaik.sha3;

/**
 * Utility class that contains amongst others important methods for dealing with
 * byte, int, and long arrays.
 * 
 * @author Christian Hanser
 */
final class Util {

  /**
   * Fills the specified sub-array of the given byte array with zeros.
   * <p>
   * Starting at the given <code>off</code> position, <code>len</code> bytes of
   * the given array are set to zero. To, for instance, set three bytes to zero,
   * starting at position 2, use: <blockquote>
   * 
   * <pre>
   * byte[] block = ...;
   * CryptoUtils.zeroBlock(block, 2, 3);
   * </pre>
   * 
   * </blockquote>
   * 
   * @param block
   *          the byte array of which some bytes have to be set to zero
   * @param off
   *          the offset indicating the start position within the byte array;
   *          the following <code>len</code> bytes are set to zero
   * @param len
   *          the number of bytes to be set to zero, starting at
   *          <code>off</code>
   */
  public static void zeroBlock(byte[] block, int off, int len) {
    for (int i = off; i < (off + len); ++i) {
      block[i] = 0;
    }
  }

  /**
   * Fills the given byte array with zeros.
   * <p>
   * 
   * @param block
   *          the byte array to be filled with zeros
   */
  public static void zeroBlock(byte[] block) {
    zeroBlock(block, 0, block.length);
  }

  /**
   * Fill an integer array with zeros.
   */
  public static void zeroBlock(int[] block) {
    Util.zeroBlock(block, 0, block.length);
  }

  /**
   * Fill part of an integer array with zeros.
   */
  public static void zeroBlock(int[] block, int off, int len) {
    for (int i = off; i < (off + len); ++i) {
      block[i] = 0;
    }
  }

  /**
   * Hidden default constructor.
   */
  private Util() {
    // hidden
  }

}
