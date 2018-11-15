package cx2x.translator.transformation.utility;

import cx2x.xcodeml.xnode.*;

import java.util.Collection;
import java.util.Optional;

/**
 * Created by chris on 05.07.17.
 */
public class CrClimHelper {
    /**
     * Return a dummy function type that is needed when adding a function
     * call that is part of and imported (USE) module.
     *
     * @param  xcodeml The current context.
     * @return The dummy function type as an Xnode.
     */
    public static String addDummyFctType(XcodeProgram xcodeml) {
        Xnode dummyFctType = new Xnode(Xcode.FFUNCTIONTYPE, xcodeml);
        dummyFctType.setAttribute(Xattr.RETURN_TYPE, Xname.TYPE_F_REAL);
        String fctType = xcodeml.getTypeTable().generateRealTypeHash();
        dummyFctType.setAttribute(Xattr.TYPE, fctType);
        xcodeml.getTypeTable().add(new Xtype(dummyFctType.element()));
        return fctType;
    }

    /**
     * Return the current module or program of the current node.
     *
     * @param  node The current node.
     * @return The module containing the node as an Xnode.
     */
    public static Optional<Xnode> getModule(Xnode node) {
        Xnode module = node.findParentModule();
        // if null then find PROGRAM, FUNCTION or SUBROUTINE node
        if (module == null) {
            Xnode pfs = node.matchAncestor(Xcode.FFUNCTIONDEFINITION);
            // if null then there is no PROGRAM or MODULE
            if (pfs == null) return Optional.empty();
            else return Optional.of(pfs);
        }
        else return Optional.of(module);
    }

    /**
     * Add a USE statement of the module named moduleName in the current module.
     *
     * @param currentModule The current module as an Xnode.
     * @param moduleName The name of the module imported by the USE statement.
     * @param xcodeml The current context.
     */
    public static void addUseStatement(Xnode currentModule, String moduleName, XcodeProgram xcodeml) {
        Xnode use = new Xnode(Xcode.FUSEDECL, xcodeml);
        use.setAttribute(Xattr.NAME, moduleName);
        currentModule.matchDirectDescendant(Xcode.DECLARATIONS).insert(use, false);
    }

    public static void addUseStatements(Xnode currentModule, Collection<String> moduleName, XcodeProgram xcodeml) {
        Xnode use = new Xnode(Xcode.FUSEDECL, xcodeml);
        moduleName.forEach(name -> use.setAttribute(Xattr.NAME, name));
        currentModule.matchDirectDescendant(Xcode.DECLARATIONS).insert(use, false);
    }
}
