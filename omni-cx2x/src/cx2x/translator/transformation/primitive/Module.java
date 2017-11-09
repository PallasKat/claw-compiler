/*
 * This file is released under terms of BSD license
 * See LICENSE file for more information
 */
package cx2x.translator.transformation.primitive;

import cx2x.translator.config.Configuration;
import cx2x.translator.language.base.ClawLanguage;
import cx2x.translator.language.helper.TransformationHelper;
import cx2x.xcodeml.exception.IllegalTransformationException;
import cx2x.xcodeml.helper.XnodeUtil;
import cx2x.xcodeml.language.DimensionDefinition;
import cx2x.xcodeml.language.InsertionPosition;
import cx2x.xcodeml.transformation.ModuleCache;
import cx2x.xcodeml.xnode.*;
import exc.xcodeml.XcodeMLtools_Fmod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.io.File;
import java.util.List;

/**
 * Primitive transformation and test and utility for XcodeML/F and CLAW enhanced
 * module files. This includes:
 * - Locate module file and load it into Xmod object.
 * - Locate CLAW enhanced module file and load it into Xmod object.
 * - Format specific suffix for module writing and search.
 *
 * @author clementval
 */
public final class Module {

  private static final String CLAW_MOD_SUFFIX = "claw";
  private static final String XMOD_FILE_EXTENSION = ".xmod";

  // Avoid instantiation of this class
  private Module() {
  }

  /**
   * Find module by name.
   *
   * @param moduleName   Name of the module.
   * @param moduleSuffix Suffix to the module name.
   * @return A Xmod object representing the module if found. Null otherwise.
   */
  private static Xmod find(String moduleName, String moduleSuffix) {
    if(moduleSuffix == null) {
      moduleSuffix = "";
    }
    for(String dir : XcodeMLtools_Fmod.getSearchPath()) {
      String path = dir + "/" + moduleName + moduleSuffix;
      File f = new File(path);
      if(f.exists()) {
        Document doc = XnodeUtil.readXmlFile(path);
        return doc != null ? new Xmod(doc, moduleName, dir) : null;
      }
    }
    return null;
  }

  /**
   * Find module by name.
   *
   * @param moduleName Name of the module.
   * @return A Xmod object representing the module if found. Null otherwise.
   */
  public static Xmod find(String moduleName) {
    return find(moduleName, XMOD_FILE_EXTENSION);
  }

  /**
   * Locate a module file generated by CLAW translator.
   *
   * @param moduleName Name of the module.
   * @return A Xmod object representing the module if found. Null otherwise.
   */
  public static Xmod findClaw(String moduleName)
  {
    return find(moduleName, getSuffix());
  }

  /**
   * Get a formatted suffix for the CLAW module file including the directive
   * and target of the current transformation.
   * .[directive].[target].claw
   *
   * @return A formatted string for the CLAW module file name.
   */
  public static String getSuffix()
  {
    StringBuilder str = new StringBuilder();
    str.append(".");
    if(Configuration.get().getCurrentDirective() != null) {
      str.append(Configuration.get().getCurrentDirective()).append(".");
    }
    if(Configuration.get().getCurrentTarget() != null) {
      str.append(Configuration.get().getCurrentTarget()).append(".");
    }
    str.append(CLAW_MOD_SUFFIX);
    str.append(XMOD_FILE_EXTENSION);
    return str.toString();
  }

  /**
   * Update the function signature in the module file to reflects local changes.
   *
   * @param moduleName  Module name to update.
   * @param xcodeml     Current XcodeML file unit.
   * @param fctDef      Function definition that has been changed.
   * @param fctType     Function type that has been changed.
   * @param claw        Pragma that has triggered the transformation.
   * @param moduleCache Current module cache.
   * @throws IllegalTransformationException If the module file or the function
   *                                        cannot be located
   */
  public static void updateSignature(String moduleName, XcodeProgram xcodeml,
                                     XfunctionDefinition fctDef,
                                     XfunctionType fctType, ClawLanguage claw,
                                     ModuleCache moduleCache,
                                     boolean importFctType)
      throws IllegalTransformationException
  {
    Xmod mod;
    if(moduleCache.isModuleLoaded(moduleName)) {
      mod = moduleCache.get(moduleName);
    } else {
      mod = fctDef.findContainingModule();
      if(mod == null) {
        throw new IllegalTransformationException(
            "Unable to locate module file for: " + moduleName,
            claw.getPragma().lineNo());
      }
      moduleCache.add(moduleName, mod);
    }

    XfunctionType fctTypeMod;
    if(importFctType) {
      // TODO should be part of XcodeML
      Node rawNode = mod.getDocument().importNode(fctType.element(), true);
      mod.getTypeTable().element().appendChild(rawNode);
      XfunctionType importedFctType = new XfunctionType((Element) rawNode);
      Xid importedFctTypeId = mod.createId(importedFctType.getType(),
          XstorageClass.F_FUNC, fctDef.getName().value());
      mod.getIdentifiers().add(importedFctTypeId);

      // check if params need to be imported as well
      if(importedFctType.getParameterNb() > 0) {
        for(Xnode param : importedFctType.getParams().getAll()) {
          mod.importType(xcodeml, param.getType());
        }
      }
      return;
    } else {
      fctTypeMod = mod.getTypeTable().getFunctionType(fctDef);
    }

    if(fctTypeMod == null) {
      /* Workaround for a bug in OMNI Compiler. Look at test case
       * claw/abstraction12. In this test case, the XcodeML/F intermediate
       * representation for the function call points to a FfunctionType element
       * with no parameters. Thus, we have to matchSeq the correct FfunctionType
       * for the same function/subroutine with the same name in the module
       * symbol table. */
      String errorMsg = "Unable to locate fct " + fctDef.getName().value() +
          " in module " + moduleName;
      int lineNo = claw.getPragma().lineNo();

      /* If not, try to matchSeq the correct FfunctionType in the module
       * definitions */
      Xid id = mod.getIdentifiers().get(fctDef.getName().value());
      if(id == null) {
        throw new IllegalTransformationException(errorMsg, lineNo);
      }
      fctTypeMod = mod.getTypeTable().getFunctionType(id);
      if(fctTypeMod == null) {
        throw new IllegalTransformationException(errorMsg, lineNo);
      }
    }

    XbasicType modIntTypeIntentIn =
        mod.createBasicType(XbuiltInType.INT, Xintent.IN);
    mod.getTypeTable().add(modIntTypeIntentIn);

    List<Xnode> paramsLocal = fctType.getParams().getAll();
    List<Xnode> paramsMod = fctTypeMod.getParams().getAll();

    if(paramsLocal.size() < paramsMod.size()) {
      throw new IllegalTransformationException(
          "Local function has more parameters than module counterpart.",
          claw.getPragma().lineNo());
    }

    for(int i = 0; i < paramsLocal.size(); ++i) {
      Xnode pLocal = paramsLocal.get(i);
      // Number of parameters in the module function as been
      if(pLocal.getBooleanAttribute(Xattr.CLAW_INSERTED)) {
        // new parameter
        Xnode param = mod.createAndAddParamIfNotExists(pLocal.value(),
            modIntTypeIntentIn.getType(), fctTypeMod);
        if(param != null) {
          param.setBooleanAttribute(Xattr.CLAW_INSERTED, true);
        }
      } else {
        Xnode pMod = paramsMod.get(i);
        String localType = pLocal.getType();
        String modType = pMod.getType();

        if(!localType.equals(modType)) {
          // Param has been updated so have to replicate the change to mod file
          XbasicType lType = xcodeml.getTypeTable().getBasicType(pLocal);
          XbasicType crtType = mod.getTypeTable().getBasicType(pMod);

          InsertionPosition insPos = InsertionPosition.
              fromString(pLocal.getAttribute(Xattr.CLAW_OVER));

          List<DimensionDefinition> dimensions =
              TransformationHelper.findDimensions(fctType, insPos);

          if(lType.isArray()) {
            String newType = TransformationHelper.duplicateWithDimension(lType,
                crtType, mod, xcodeml, dimensions);
            pMod.setType(newType);
          }
        }

        // Propagate the over attribute
        pLocal.copyAttribute(pMod, Xattr.CLAW_OVER);
      }
    }
  }
}
