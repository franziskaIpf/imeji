package de.mpg.imeji.logic.validation;

import de.mpg.imeji.logic.validation.impl.AlbumValidator;
import de.mpg.imeji.logic.validation.impl.CollectionValidator;
import de.mpg.imeji.logic.validation.impl.ItemValidator;
import de.mpg.imeji.logic.validation.impl.MetadataValidator;
import de.mpg.imeji.logic.validation.impl.ProfileValidator;
import de.mpg.imeji.logic.validation.impl.PseudoValidator;
import de.mpg.imeji.logic.validation.impl.SpaceValidator;
import de.mpg.imeji.logic.validation.impl.UserGroupValidator;
import de.mpg.imeji.logic.validation.impl.UserValidator;
import de.mpg.imeji.logic.validation.impl.Validator;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;

/**
 * Factory for {@link Validator}
 *
 * @author saquet
 *
 */
public class ValidatorFactory {

  private ValidatorFactory() {
    // avoid constructor
  }

  /**
   * Return a new {@link Validator} according to the object class
   *
   * @param <T>
   *
   * @param t
   * @return
   */
  public static Validator<?> newValidator(Object obj, Validator.Method method) {
    Validator<?> validator = new PseudoValidator();;
    // For now, do not do anything with Delete, just a possibility
    if (Validator.Method.DELETE.equals(method)) {
      return validator;
    }
    if (obj instanceof Item) {
      validator = new ItemValidator();
    } else if (obj instanceof Metadata) {
      validator = new MetadataValidator();
    } else if (obj instanceof CollectionImeji) {
      validator = new CollectionValidator();
    } else if (obj instanceof Album) {
      validator = new AlbumValidator();
    } else if (obj instanceof MetadataProfile) {
      validator = new ProfileValidator();
    } else if (obj instanceof User) {
      validator = new UserValidator();
    } else if (obj instanceof Space) {
      validator = new SpaceValidator();
    } else if (obj instanceof UserGroup) {
      validator = new UserGroupValidator();
    }
    return validator;
  }
}
