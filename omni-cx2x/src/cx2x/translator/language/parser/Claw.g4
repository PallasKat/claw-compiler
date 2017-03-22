/*
 * This file is released under terms of BSD license
 * See LICENSE file for more information
 */

/**
 * ANTLR 4 Grammar file for the CLAW directive language.
 *
 * @author clementval
 */
grammar Claw;

@header
{
import cx2x.translator.common.ClawConstant;
import cx2x.translator.language.base.*;
import cx2x.translator.language.common.*;
import cx2x.translator.language.helper.target.Target;
import cx2x.translator.common.Utility;
}

/*----------------------------------------------------------------------------
 * PARSER RULES
 *----------------------------------------------------------------------------*/


/*
 * Entry point for the analyzis of a CLAW directive.
 * Return a CLawLanguage object with all needed information.
 */
analyze returns [ClawLanguage l]
  @init{
    $l = new ClawLanguage();
  }
  :
    CLAW directive[$l] EOF
  | CLAW VERBATIM // this directive accept anything after the verbatim
    { $l.setDirective(ClawDirective.VERBATIM); }
  | CLAW ACC // this directive accept anything after the acc
    { $l.setDirective(ClawDirective.PRIMITIVE); }
  | CLAW OMP // this directive accept anything after the omp
    { $l.setDirective(ClawDirective.PRIMITIVE); }
;

directive[ClawLanguage l]
  @init{
    List<ClawMapping> m = new ArrayList<>();
    List<String> o = new ArrayList<>();
    List<String> s = new ArrayList<>();
  }
  :

  // loop-fusion directive
  // !$claw loop-fusion [group(group_id)] [collapse(n)]
    LOOP_FUSION loop_fusion_clauses[$l] EOF
    { $l.setDirective(ClawDirective.LOOP_FUSION); }

  // loop-interchange directive
  // !$claw loop-interchange [(induction_var[, induction_var] ...)]
  | LOOP_INTERCHANGE loop_interchange_clauses[$l] EOF
    { $l.setDirective(ClawDirective.LOOP_INTERCHANGE); }

  // loop-extract directive
  /* !$claw loop-extract range(range_info)
   * [[map(var[,var]...:mapping[,mapping]...)
   * [map(var[,var]...:mapping[,mapping]...)] ...]
   * [fusion [group(group_id)]] [ parallel] [acc(directives)] */
  | LOOP_EXTRACT range_option mapping_option_list[m] loop_extract_clauses[$l] EOF
    {
      $l.setDirective(ClawDirective.LOOP_EXTRACT);
      $l.setRange($range_option.r);
      $l.setMappings(m);
    }

  // remove directive
  | REMOVE (target_clause[$l])? EOF
    { $l.setDirective(ClawDirective.REMOVE); }
  | END REMOVE EOF
    {
      $l.setDirective(ClawDirective.REMOVE);
      $l.setEndPragma();
    }

  // Kcache directive
  | KCACHE data_clause[$l] kcache_clauses[$l] EOF
    {
      $l.setDirective(ClawDirective.KCACHE);
    }

  // Array notation transformation directive
  /* !$claw array-transform [induction(name [[,] name]...)] &
   * !$claw [fusion [group(group_id)]] [parallel] &
   * !$claw [acc([clause [[,] clause]...])] */
  | ARRAY_TRANS array_transform_clauses[$l] EOF
    {  $l.setDirective(ClawDirective.ARRAY_TRANSFORM); }
  // [!$claw end array-transform]
  | END ARRAY_TRANS
    {
      $l.setDirective(ClawDirective.ARRAY_TRANSFORM);
      $l.setEndPragma();
    }

  // loop-hoist directive
  /* !$claw loop-hoist(induction_var[[, induction_var] ...]) &
   * !$claw [reshape(array_name( target_dimension[,kept_dimensions]))] &
   * !$claw [interchange [(induction_var[[, induction_var] ...])]] */
  | LOOP_HOIST '(' ids_list[o] ')' loop_hoist_clauses[$l] EOF
    {
      $l.setHoistInductionVars(o);
      $l.setDirective(ClawDirective.LOOP_HOIST);
    }
  // !$claw end loop-hoist
  | END LOOP_HOIST EOF
    {
      $l.setDirective(ClawDirective.LOOP_HOIST);
      $l.setEndPragma();
    }
  // on the fly directive
  // !$claw call array_name=function_call(arg_list)
  | ARRAY_TO_CALL array_name=IDENTIFIER '=' fct_name=IDENTIFIER '(' identifiers_list[o] ')' (target_clause[$l])?
    {
      $l.setDirective(ClawDirective.ARRAY_TO_CALL);
      $l.setFctParams(o);
      $l.setFctName($fct_name.text);
      $l.setArrayName($array_name.text);
    }

   // parallelize directive
   /* !$claw define dimension dim_id(lower_bound:upper_bound) &
    * [!$claw define dimension dim_id(lower_bound:upper_bound) &] ...
    * !$claw parallelize [data(data(var_1[,var_2] ...)]
    * [over (dim_id|:[,dim_id|:]...)] */
   | define_option[$l]* PARALLELIZE data_over_clause[$l]* parallelize_clauses[$l]
     {
       $l.setDirective(ClawDirective.PARALLELIZE);
     }
   // !$claw parallelize forward
   | PARALLELIZE FORWARD parallelize_clauses[$l]
     {
       $l.setDirective(ClawDirective.PARALLELIZE);
       $l.setForwardClause();
     }
   // TODO is it used ?
   | END PARALLELIZE
     {
       $l.setDirective(ClawDirective.PARALLELIZE);
       $l.setEndPragma();
     }

   // ignore directive
   // !$claw ignore
   | IGNORE
     {
       $l.setDirective(ClawDirective.IGNORE);
     }
   // !$claw end ignore
   | END IGNORE
     {
       $l.setDirective(ClawDirective.IGNORE);
       $l.setEndPragma();
     }
;

// Comma-separated identifiers list
ids_list[List<String> ids]
  :
    i=IDENTIFIER { $ids.add($i.text); }
  | i=IDENTIFIER { $ids.add($i.text); } ',' ids_list[$ids]
;

// Comma-separated identifiers or colon symbol list
ids_or_colon_list[List<String> ids]
  :
    i=IDENTIFIER { $ids.add($i.text); }
  | ':' { $ids.add(":"); }
  | i=IDENTIFIER { $ids.add($i.text); } ',' ids_or_colon_list[$ids]
  | ':' { $ids.add(":"); } ',' ids_or_colon_list[$ids]
;

// data over clause used in parallelize directive
data_over_clause[ClawLanguage l]
  @init{
    List<String> overLst = new ArrayList<>();
    List<String> dataLst = new ArrayList<>();
  }
:
  DATA '(' ids_list[dataLst] ')' OVER '(' ids_or_colon_list[overLst] ')'
  {
    $l.setOverDataClause(dataLst);
    $l.setOverClause(overLst);
  }
;

// group clause
group_clause[ClawLanguage l]:
    GROUP '(' group_name=IDENTIFIER ')'
    { $l.setGroupClause($group_name.text); }
;

// collapse clause
collapse_clause[ClawLanguage l]:
    COLLAPSE '(' n=NUMBER ')'
    { $l.setCollapseClause($n.text); }
;

// fusion clause
fusion_clause[ClawLanguage l]:
    FUSION (group_clause[$l])? { $l.setFusionClause(); }
;

// parallel clause
parallel_clause[ClawLanguage l]:
    PARALLEL { $l.setParallelClause(); }
;

// acc clause
acc_clause[ClawLanguage l]
  @init{
    List<String> tempAcc = new ArrayList<>();
  }
  :
    ACC '(' identifiers[tempAcc] ')' { $l.setAcceleratorClauses(Utility.join(" ", tempAcc)); }
;

// interchange clause
interchange_clause[ClawLanguage l]:
    INTERCHANGE indexes_option[$l] { $l.setInterchangeClause(); }
;

// induction clause
induction_clause[ClawLanguage l]
  @init{
    List<String> temp = new ArrayList<>();
  }
  :
    INDUCTION '(' ids_list[temp] ')' { $l.setInductionClause(temp); }
;

// data clause
data_clause[ClawLanguage l]
  @init {
    List<String> temp = new ArrayList<>();
  }
  :
    DATA '(' ids_list[temp] ')' { $l.setDataClause(temp); }
;

// private clause
private_clause[ClawLanguage l]:
    PRIVATE { $l.setPrivateClause(); }
;

// reshape clause
reshape_clause[ClawLanguage l]
  @init{
    List<ClawReshapeInfo> r = new ArrayList();
  }
  :
    RESHAPE '(' reshape_list[r] ')'
    { $l.setReshapeClauseValues(r); }
;

// reshape clause
reshape_element returns [ClawReshapeInfo i]
  @init{
    List<Integer> temp = new ArrayList();
  }
:
    array_name=IDENTIFIER '(' target_dim=NUMBER ')'
    { $i = new ClawReshapeInfo($array_name.text, Integer.parseInt($target_dim.text), temp); }
  | array_name=IDENTIFIER '(' target_dim=NUMBER ',' integers_list[temp] ')'
    { $i = new ClawReshapeInfo($array_name.text, Integer.parseInt($target_dim.text), temp); }
;

reshape_list[List<ClawReshapeInfo> r]:
    info=reshape_element { $r.add($info.i); } ',' reshape_list[$r]
  | info=reshape_element { $r.add($info.i); }
;

identifiers[List<String> ids]:
    i=IDENTIFIER { $ids.add($i.text); }
  | i=IDENTIFIER { $ids.add($i.text); } identifiers[$ids]
;

identifiers_list[List<String> ids]:
    i=IDENTIFIER { $ids.add($i.text); }
  | i=IDENTIFIER { $ids.add($i.text); } ',' identifiers_list[$ids]
;

integers[List<Integer> ints]:

;

integers_list[List<Integer> ints]:
    i=NUMBER { $ints.add(Integer.parseInt($i.text)); }
  | i=NUMBER { $ints.add(Integer.parseInt($i.text)); } ',' integers[$ints]
;

indexes_option[ClawLanguage l]
  @init{
    List<String> indexes = new ArrayList();
  }
  :
    '(' ids_list[indexes] ')' { $l.setIndexes(indexes); }
  | /* empty */
;

offset_clause[List<Integer> offsets]:
    OFFSET '(' offset_list[$offsets] ')'
;

offset_list[List<Integer> offsets]:
    offset[$offsets]
  | offset[$offsets] ',' offset_list[$offsets]
;

offset[List<Integer> offsets]:
    n=NUMBER { $offsets.add(Integer.parseInt($n.text)); }
  | '-' n=NUMBER { $offsets.add(-Integer.parseInt($n.text)); }
  | '+' n=NUMBER { $offsets.add(Integer.parseInt($n.text)); }
;


range_option returns [ClawRange r]
  @init{
    $r = new ClawRange();
  }
  :
    RANGE '(' induction_var=IDENTIFIER '=' lower=range_id ',' upper=range_id ')'
    {
      $r.setInductionVar($induction_var.text);
      $r.setLowerBound($lower.text);
      $r.setUpperBound($upper.text);
      $r.setStep(ClawConstant.DEFAULT_STEP_VALUE);
    }
  | RANGE '(' induction_var=IDENTIFIER '=' lower=range_id ',' upper=range_id ',' step=range_id ')'
    {
      $r.setInductionVar($induction_var.text);
      $r.setLowerBound($lower.text);
      $r.setUpperBound($upper.text);
      $r.setStep($step.text);
    }
;

range_id returns [String text]:
    n=NUMBER { $text = $n.text; }
  | i=IDENTIFIER { $text = $i.text; }
;


mapping_var returns [ClawMappingVar mappingVar]:
    lhs=IDENTIFIER '/' rhs=IDENTIFIER
    {
      $mappingVar = new ClawMappingVar($lhs.text, $rhs.text);
    }
  | i=IDENTIFIER
    {
      $mappingVar = new ClawMappingVar($i.text, $i.text);
    }
;


mapping_var_list[List<ClawMappingVar> vars]:
     mv=mapping_var { $vars.add($mv.mappingVar); }
   | mv=mapping_var { $vars.add($mv.mappingVar); } ',' mapping_var_list[$vars]
;


mapping_option returns [ClawMapping mapping]
  @init{
    $mapping = new ClawMapping();
    List<ClawMappingVar> listMapped = new ArrayList<ClawMappingVar>();
    List<ClawMappingVar> listMapping = new ArrayList<ClawMappingVar>();
    $mapping.setMappedVariables(listMapped);
    $mapping.setMappingVariables(listMapping);
  }
  :
    MAP '(' mapping_var_list[listMapped] ':' mapping_var_list[listMapping] ')'
;

mapping_option_list[List<ClawMapping> mappings]:
    m=mapping_option { $mappings.add($m.mapping); }
  | m=mapping_option { $mappings.add($m.mapping); } mapping_option_list[$mappings]
;


define_option[ClawLanguage l]:
    DEFINE DIMENSION id=IDENTIFIER '(' lower=range_id ':' upper=range_id ')'
    {
      ClawDimension cd = new ClawDimension($id.text, $lower.text, $upper.text);
      $l.addDimension(cd);
    }
;

// Allow to switch order
parallelize_clauses[ClawLanguage l]:
    copy_clause_optional[$l] update_clause_optional[$l]
  | update_clause_optional[$l] copy_clause_optional[$l]
;

copy_clause_optional[ClawLanguage l]:
    /* empty */
  | COPY
    { $l.setCopyClauseValue(ClawDMD.BOTH); }
  | COPY '(' IN ')'
    { $l.setCopyClauseValue(ClawDMD.IN); }
  | COPY '(' OUT ')'
    { $l.setCopyClauseValue(ClawDMD.OUT); }
;

update_clause_optional[ClawLanguage l]:
    /* empty */
  | UPDATE
    { $l.setUpdateClauseValue(ClawDMD.BOTH); }
  | UPDATE '(' IN ')'
    { $l.setUpdateClauseValue(ClawDMD.IN); }
  | UPDATE '(' OUT ')'
    { $l.setUpdateClauseValue(ClawDMD.OUT); }
;

target_clause[ClawLanguage l]
  @init{
    List<Target> targets = new ArrayList<>();
  }
  :
    TARGET '(' target_list[targets] ')'
    { $l.setTargetClauseValue(targets); }
;

target_list[List<Target> targets]:
    target { if(!$targets.contains($target.t)) { $targets.add($target.t); } }
  | target { if(!$targets.contains($target.t)) { $targets.add($target.t); } } ',' target_list[$targets]
;


target returns [Target t]:
    CPU { $t = Target.CPU; }
  | GPU { $t = Target.GPU; }
  | MIC { $t = Target.MIC; }
;

// Possible permutation of clauses for the loop-fusion directive
loop_fusion_clauses[ClawLanguage l]:
  (
    { !$l.hasGroupClause() }?    group_clause[$l]
  | { !$l.hasCollapseClause() }? collapse_clause[$l]
  | { !$l.hasTargetClause() }?   target_clause[$l]
  )*
;

// Possible permutation of clauses for the loop-interchange directive
loop_interchange_clauses[ClawLanguage l]:
  indexes_option[$l]
  (
    { !$l.hasParallelClause() }?    parallel_clause[$l]
  | { !$l.hasAcceleratorClause() }? acc_clause[$l]
  | { !$l.hasTargetClause() }?      target_clause[$l]
  )*
;

// Possible permutation of clauses for the loop-extract directive
loop_extract_clauses[ClawLanguage l]:
  (
    { !$l.hasFusionClause() }?      fusion_clause[$l]
  | { !$l.hasParallelClause() }?    parallel_clause[$l]
  | { !$l.hasAcceleratorClause() }? acc_clause[$l]
  | { !$l.hasTargetClause() }?      target_clause[$l]
  )*
;

// Possible permutation of clauses for the array-transform directive
array_transform_clauses[ClawLanguage l]:
  (
    { !$l.hasFusionClause() }?      fusion_clause[$l]
  | { !$l.hasParallelClause() }?    parallel_clause[$l]
  | { !$l.hasAcceleratorClause() }? acc_clause[$l]
  | { !$l.hasInductionClause() }?   induction_clause[$l]
  | { !$l.hasTargetClause() }?      target_clause[$l]
  )*
;

// Possible permutation of clauses for the kcache directive
kcache_clauses[ClawLanguage l]
@init{
    List<Integer> i = new ArrayList<>();
}
:
  (
    { $l.getOffsets() == null }? offset_clause[i] { $l.setOffsets(i); }
  | { !$l.hasInitClause() }?     INIT { $l.setInitClause(); }
  | { !$l.hasPrivateClause() }?  private_clause[$l]
  | { !$l.hasTargetClause() }?   target_clause[$l]
  )*
  {
    if($l.getOffsets() == null) {
      $l.setOffsets(i); // Set default offset if not specified
    }
  }
;

// Possible permutation of clauses for the loop-hoist directive
loop_hoist_clauses[ClawLanguage l]:
  (
    { !$l.hasReshapeClause() }?     reshape_clause[$l]
  | { !$l.hasInterchangeClause() }? interchange_clause[$l]
  | { !$l.hasTargetClause() }?      target_clause[$l]
  )*
;


/*----------------------------------------------------------------------------
 * LEXER RULES
 *----------------------------------------------------------------------------*/

// Start point
CLAW         : 'claw';

// CLAW Directives
ARRAY_TRANS      : 'array-transform';
ARRAY_TO_CALL    : 'call';
DEFINE           : 'define';
END              : 'end';
KCACHE           : 'kcache';
LOOP_EXTRACT     : 'loop-extract';
LOOP_FUSION      : 'loop-fusion';
LOOP_HOIST       : 'loop-hoist';
LOOP_INTERCHANGE : 'loop-interchange';
PARALLELIZE      : 'parallelize';
REMOVE           : 'remove';
IGNORE           : 'ignore';
VERBATIM         : 'verbatim';


// CLAW Clauses
COLLAPSE     : 'collapse';
COPY         : 'copy';
DATA         : 'data';
DIMENSION    : 'dimension';
FORWARD      : 'forward';
FUSION       : 'fusion';
GROUP        : 'group';
INDUCTION    : 'induction';
INIT         : 'init';
INTERCHANGE  : 'interchange';
MAP          : 'map';
OFFSET       : 'offset';
OVER         : 'over';
PARALLEL     : 'parallel';
PRIVATE      : 'private';
RANGE        : 'range';
RESHAPE      : 'reshape';
TARGET       : 'target';
UPDATE       : 'update';

// data copy/update clause keywords
IN           : 'in';
OUT          : 'out';

// Directive primitive clause
ACC          : 'acc';
OMP          : 'omp';

// Target for the target clause
CPU          : 'cpu';
GPU          : 'gpu';
MIC          : 'mic';

// Special elements
IDENTIFIER      : [a-zA-Z_$] [a-zA-Z_$0-9-]* ;
NUMBER          : (DIGIT)+ ;
fragment DIGIT  : [0-9] ;

// Skip whitspaces
WHITESPACE   : ( '\t' | ' ' | '\r' | '\n'| '\u000C' )+ { skip(); };
