export interface Class {
  id: string;
  fullName: string;
  simpleName: string;
  packageName: string;
  unused: boolean;
  framework: boolean;
  test: boolean;
  dependsOn: Dependency[];
}

export interface Method {
  declaringClass: string;
  name: string;
  fullName: string;
  called: boolean;
  framework: boolean;
  test: boolean;
  unused: boolean;
  calls?: string[];
}

export interface Dependency {
  target: string;
  type: 'IMPORT' | 'REFERENCE';
}

export interface UnusedCode {
  classes: {
    id: string;
    fullName: string;
    reason: string;
  }[];
  methods: {
    id: string;
    className: string;
    methodName: string;
    reason: string;
  }[];
}

export interface ImpactAnalysis {
  class?: string;
  method?: string;
  type: 'class' | 'method';
  impactRadius: {
    directlyAffected: string[];
    indirectlyAffected: string[];
    totalImpact: number;
    severityLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
  };
}

export interface CallGraph {
  nodes: Array<{
    id: string;
    type: string;
  }>;
  edges: Array<{
    from: string;
    to: string;
  }>;
}

export interface CodeData {
  classes: Class[];
  methods: Method[];
  unusedCode: UnusedCode;
  impactAnalysis: ImpactAnalysis[];
  callGraph: CallGraph;
}